#!/usr/bin/env python3
"""Rebuild the local MinIO image from a pinned official source release."""
import hashlib, json, os, pathlib, shutil, subprocess, tarfile

PROJECT = pathlib.Path(__file__).resolve().parents[1]
RUNTIME = PROJECT.parents[1]/'.local-runtime'
TAG = 'RELEASE.2025-10-15T17-29-55Z'
SOURCE_SHA256 = 'be6d0bd3696c3a13a35f02d3a0280b64319c67918b4501c5c3d87f96d000085c'

def download(url, path):
    subprocess.run(['curl','-fsSL','--retry','3','--max-time','600','-o',str(path),url],check=True)

def verify(path, checksum):
    with path.open('rb') as stream:
        if hashlib.file_digest(stream,'sha256').hexdigest()!=checksum:
            raise RuntimeError('Checksum mismatch: '+str(path))

def main():
    downloads=RUNTIME/'downloads';downloads.mkdir(parents=True,exist_ok=True)
    go=RUNTIME/'go/bin/go'
    if not go.exists():
        metadata=downloads/'go-release.json';download('https://go.dev/dl/?mode=json',metadata)
        release=next(x for x in json.loads(metadata.read_text()) if x['stable'])
        item=next(x for x in release['files'] if x['os']=='linux' and x['arch']=='amd64' and x['kind']=='archive')
        archive=downloads/item['filename'];download('https://go.dev/dl/'+item['filename'],archive);verify(archive,item['sha256'])
        with tarfile.open(archive) as tf:tf.extractall(RUNTIME,filter='data')
    archive=downloads/'minio-source.tar.gz'
    if not archive.exists():download(f'https://github.com/minio/minio/archive/refs/tags/{TAG}.tar.gz',archive)
    verify(archive,SOURCE_SHA256)
    source=RUNTIME/('minio-'+TAG)
    if not source.exists():
        with tarfile.open(archive) as tf:tf.extractall(RUNTIME,filter='data')
    context=RUNTIME/'minio-build';context.mkdir(exist_ok=True)
    env=os.environ.copy()
    env.update({'CGO_ENABLED':'0','GOMAXPROCS':'8','GOPATH':str(RUNTIME/'go-path'),'GOCACHE':str(RUNTIME/'go-cache')})
    subprocess.run([str(go),'build','-trimpath','-o',str(context/'minio'),'.'],cwd=source,env=env,check=True)
    shutil.copyfile('/etc/ssl/certs/ca-certificates.crt',context/'ca-certificates.crt')
    subprocess.run(['docker','build','-t','rag-local/minio:'+TAG,'-f',str(PROJECT/'docker/minio-local/Dockerfile'),str(context)],check=True)

if __name__=='__main__':main()
