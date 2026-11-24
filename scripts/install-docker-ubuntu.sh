#!/usr/bin/env bash
# Run in the host's normal terminal: bash scripts/install-docker-ubuntu.sh
# Follows https://docs.docker.com/engine/install/ubuntu/
set -euo pipefail
source /etc/os-release
if [[ "$ID" != ubuntu || "$(dpkg --print-architecture)" != amd64 ]]; then
  echo 'This script is intended for the checked Ubuntu amd64 test host.' >&2
  exit 1
fi
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL --retry 3 https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$(id -un)"
sudo docker version
sudo docker compose version
echo 'Docker installed; current user added to the docker group.'
echo 'New login sessions can use docker directly; existing sessions can use sg docker -c COMMAND.'
