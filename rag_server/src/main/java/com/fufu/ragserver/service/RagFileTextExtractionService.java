package com.fufu.ragserver.service;

import com.fufu.ragserver.config.RagFileProperties;
import com.fufu.ragserver.domain.RagFileTextExtractionResult;
import com.fufu.ragserver.exception.ServiceException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.tesseract.global.tesseract.OEM_LSTM_ONLY;
import static org.bytedeco.tesseract.global.tesseract.PSM_AUTO;
import static org.bytedeco.tesseract.global.tesseract.TessDeleteText;

/**
 * 将上传文件统一预处理为可分块的纯文本。
 *
 * @author fufu
 * @date 2026-07-10
 */
@Service
public class RagFileTextExtractionService
{
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(
            Arrays.asList("TXT", "MD", "CSV", "JSON", "LOG"));
    private static final Set<String> TIKA_TEXT_EXTENSIONS = new HashSet<>(
            Arrays.asList("PDF", "DOC", "DOCX", "RTF", "TXT", "MD", "CSV", "JSON", "LOG"));
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            Arrays.asList("PNG", "JPG", "JPEG", "BMP", "GIF", "TIF", "TIFF", "WEBP"));

    private final RagFileProperties properties;
    private final AutoDetectParser parser = new AutoDetectParser();
    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

    public RagFileTextExtractionService(RagFileProperties properties)
    {
        this.properties = properties;
    }

    public RagFileTextExtractionResult extract(String fileName, String contentType, byte[] rawContent)
    {
        String fileType = extension(fileName);
        if (rawContent == null || rawContent.length == 0)
        {
            throw new ServiceException("上传文件不能为空");
        }
        if (!isSupported(fileType))
        {
            throw new ServiceException("暂不支持该文件格式：" + fileType);
        }

        String text;
        String parseMethod;
        if ("OFD".equals(fileType))
        {
            OfdExtraction ofdExtraction = extractOfd(rawContent);
            text = ofdExtraction.text;
            parseMethod = ofdExtraction.parseMethod;
        }
        else
        {
            boolean imageOcr = IMAGE_EXTENSIONS.contains(fileType);
            if ("PDF".equals(fileType))
            {
                PdfExtraction pdfExtraction = extractPdf(fileName, contentType, rawContent);
                text = pdfExtraction.text;
                parseMethod = pdfExtraction.parseMethod;
            }
            else
            {
                text = imageOcr ? extractImageByPortableTesseract(rawContent) : extractByTika(fileName, contentType, rawContent);
                parseMethod = imageOcr ? "PORTABLE_TESSERACT_OCR" : parseMethodForText(fileType);
            }
        }

        text = normalizeText(text);
        if (StringUtils.isBlank(text))
        {
            throw new ServiceException("文件未抽取到有效文本内容：" + fileName);
        }

        RagFileTextExtractionResult result = new RagFileTextExtractionResult();
        result.setText(text);
        result.setFileType(fileType);
        result.setParseMethod(parseMethod);
        return result;
    }

    private OfdExtraction extractOfd(byte[] rawContent)
    {
        String ofdrwError = null;
        try (OFDReader reader = new OFDReader(new ByteArrayInputStream(rawContent)))
        {
            ContentExtractor extractor = new ContentExtractor(reader);
            List<String> contents = extractor.extractAll();
            String text = contents.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining("\n"));
            if (StringUtils.isNotBlank(text))
            {
                return new OfdExtraction(text, "OFD_TEXT_EXTRACTION");
            }
        }
        catch (Exception e)
        {
            ofdrwError = e.getMessage();
        }

        String zipText = extractOfdXmlText(rawContent);
        if (StringUtils.isNotBlank(zipText))
        {
            return new OfdExtraction(zipText, "OFD_ZIP_XML_TEXT_EXTRACTION");
        }

        if (StringUtils.isNotBlank(ofdrwError))
        {
            throw new ServiceException("OFD文本抽取失败，ZIP/XML兜底也未识别到有效文本：" + ofdrwError);
        }
        throw new ServiceException("OFD未抽取到有效文本内容，且ZIP/XML兜底未识别到有效文本");
    }

    private String extractOfdXmlText(byte[] rawContent)
    {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(rawContent)))
        {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".xml"))
                {
                    continue;
                }
                appendXmlCharacters(zipInputStream, builder);
                zipInputStream.closeEntry();
            }
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new ServiceException("OFD ZIP/XML文本抽取失败：" + e.getMessage());
        }
    }

    private void appendXmlCharacters(InputStream inputStream, StringBuilder builder) throws Exception
    {
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);
        try
        {
            while (reader.hasNext())
            {
                int event = reader.next();
                if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)
                {
                    String text = reader.getText();
                    if (StringUtils.isNotBlank(text))
                    {
                        builder.append(text.trim()).append('\n');
                    }
                }
            }
        }
        finally
        {
            reader.close();
        }
    }

    private String extractByTika(String fileName, String contentType, byte[] rawContent)
    {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(rawContent))
        {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            if (StringUtils.isNotBlank(fileName))
            {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            if (StringUtils.isNotBlank(contentType))
            {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }

            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        }
        catch (Exception e)
        {
            throw new ServiceException("文件文本抽取失败：" + e.getMessage());
        }
    }

    private PdfExtraction extractPdf(String fileName, String contentType, byte[] rawContent)
    {
        String tikaError = null;
        try
        {
            String tikaText = normalizeText(extractByTika(fileName, contentType, rawContent));
            if (StringUtils.isNotBlank(tikaText))
            {
                return new PdfExtraction(tikaText, "TIKA_PDF_TEXT_EXTRACTION");
            }
        }
        catch (ServiceException e)
        {
            tikaError = e.getMessage();
        }
        catch (LinkageError e)
        {
            tikaError = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        String ocrText = normalizeText(extractPdfByOcr(rawContent));
        if (StringUtils.isNotBlank(ocrText))
        {
            return new PdfExtraction(ocrText, "PDF_PAGE_OCR");
        }

        if (StringUtils.isNotBlank(tikaError))
        {
            throw new ServiceException("PDF文本抽取失败，OCR也未识别到有效文本：" + tikaError);
        }
        throw new ServiceException("PDF未抽取到有效文本内容，且OCR未识别到有效文本");
    }

    private String extractPdfByOcr(byte[] rawContent)
    {
        try (PDDocument document = Loader.loadPDF(rawContent))
        {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < document.getNumberOfPages(); i++)
            {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
                {
                    ImageIO.write(image, "png", outputStream);
                    String pageText = extractImageByPortableTesseract(outputStream.toByteArray());
                    if (StringUtils.isNotBlank(pageText))
                    {
                        builder.append(pageText).append('\n');
                    }
                }
            }
            return builder.toString();
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("PDF OCR失败：" + e.getMessage());
        }
    }

    private String extractImageByPortableTesseract(byte[] rawContent)
    {
        Path tessDataPath = resolveTessDataPath();
        TessBaseAPI api = new TessBaseAPI();
        PIX pix = null;
        BytePointer text = null;
        try
        {
            if (api.Init(tessDataPath.toString(), properties.getOcrLanguage(), OEM_LSTM_ONLY) != 0)
            {
                throw new ServiceException("OCR初始化失败，请检查tessdata语言模型：" + properties.getOcrLanguage());
            }
            api.SetPageSegMode(PSM_AUTO);
            api.SetVariable("debug_file", "/dev/null");

            pix = pixReadMem(rawContent, rawContent.length);
            if (pix == null || pix.isNull())
            {
                throw new ServiceException("图片格式无法被OCR引擎读取");
            }

            api.SetImage(pix);
            text = api.GetUTF8Text();
            return text == null || text.isNull() ? "" : text.getString(StandardCharsets.UTF_8.name());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("图片OCR失败：" + e.getMessage());
        }
        finally
        {
            if (text != null)
            {
                TessDeleteText(text);
            }
            if (pix != null && !pix.isNull())
            {
                pixDestroy(pix);
            }
            api.End();
            api.close();
        }
    }

    private Path resolveTessDataPath()
    {
        if (StringUtils.isNotBlank(properties.getOcrDataPath()))
        {
            Path configuredPath = Paths.get(properties.getOcrDataPath());
            if (!Files.isDirectory(configuredPath))
            {
                throw new ServiceException("OCR语言模型目录不存在：" + configuredPath);
            }
            return configuredPath;
        }

        try
        {
            Path targetDir = Paths.get(System.getProperty("java.io.tmpdir"), "rag-server-tessdata");
            Files.createDirectories(targetDir);
            for (String language : properties.getOcrLanguage().split("\\+"))
            {
                String normalizedLanguage = language.trim();
                if (StringUtils.isBlank(normalizedLanguage))
                {
                    continue;
                }
                copyTessDataIfNecessary(normalizedLanguage, targetDir);
            }
            return targetDir;
        }
        catch (IOException e)
        {
            throw new ServiceException("准备OCR语言模型失败：" + e.getMessage());
        }
    }

    private void copyTessDataIfNecessary(String language, Path targetDir) throws IOException
    {
        Path targetFile = targetDir.resolve(language + ".traineddata");
        if (Files.exists(targetFile) && Files.size(targetFile) > 0)
        {
            return;
        }

        String resourcePath = "tessdata/" + language + ".traineddata";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath))
        {
            if (inputStream == null)
            {
                throw new ServiceException("缺少OCR语言模型资源：" + resourcePath);
            }
            Files.copy(inputStream, targetFile);
        }
    }

    private boolean isSupported(String fileType)
    {
        return TIKA_TEXT_EXTENSIONS.contains(fileType) || IMAGE_EXTENSIONS.contains(fileType) || "OFD".equals(fileType);
    }

    private String parseMethodForText(String fileType)
    {
        return TEXT_EXTENSIONS.contains(fileType) ? "TIKA_TEXT" : "TIKA_TEXT_EXTRACTION";
    }

    private String normalizeText(String text)
    {
        return StringUtils.defaultString(text).replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String extension(String fileName)
    {
        if (StringUtils.isBlank(fileName) || !fileName.contains("."))
        {
            return "UNKNOWN";
        }
        return StringUtils.substringAfterLast(fileName, ".").toUpperCase(Locale.ROOT);
    }

    private static class PdfExtraction
    {
        private final String text;
        private final String parseMethod;

        private PdfExtraction(String text, String parseMethod)
        {
            this.text = text;
            this.parseMethod = parseMethod;
        }
    }

    private static class OfdExtraction
    {
        private final String text;
        private final String parseMethod;

        private OfdExtraction(String text, String parseMethod)
        {
            this.text = text;
            this.parseMethod = parseMethod;
        }
    }
}
