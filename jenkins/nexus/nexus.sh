#!/bin/bash
# =============================================================================
# Nexus Repository OSS 3.84.0-03 Installation Script
# Platform: Amazon Linux 2023
# =============================================================================
# Usage: sudo bash nexus-install.sh
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
NEXUS_VERSION="3.84.0-03"
NEXUS_TAR="nexus-${NEXUS_VERSION}.tar.gz"
NEXUS_DOWNLOAD_URL="https://download.sonatype.com/nexus/3/nexus-${NEXUS_VERSION}-linux-x86_64.tar.gz"
NEXUS_INSTALL_DIR="/opt/nexus-${NEXUS_VERSION}"
NEXUS_SYMLINK="/opt/nexus"
SONATYPE_WORK="/opt/sonatype-work"

# ── 1. System Update & Prerequisites ──────────────────────────────────────────
echo ">>> [1/6] Updating system and installing prerequisites..."
dnf update -y
dnf install -y java-21-amazon-corretto wget tar

# ── 2. Create Dedicated 'nexus' User ──────────────────────────────────────────
echo ">>> [2/6] Creating dedicated nexus user..."
if id "nexus" &>/dev/null; then
    echo "    User 'nexus' already exists, skipping..."
else
    useradd -r -M -d /opt/nexus -s /sbin/nologin nexus
    echo "    User 'nexus' created."
fi

# ── 3. Download & Unpack Nexus ─────────────────────────────────────────────────
echo ">>> [3/6] Downloading Nexus ${NEXUS_VERSION}..."
cd /opt
wget -O "${NEXUS_TAR}" "${NEXUS_DOWNLOAD_URL}"

echo "    Extracting..."
tar -xzf "${NEXUS_TAR}"

echo "    Creating symlink /opt/nexus → ${NEXUS_INSTALL_DIR}..."
ln -sfn "${NEXUS_INSTALL_DIR}" "${NEXUS_SYMLINK}"

echo "    Creating sonatype-work directory..."
mkdir -p "${SONATYPE_WORK}"

echo "    Setting ownership..."
chown -R nexus:nexus "${NEXUS_SYMLINK}" "${NEXUS_INSTALL_DIR}" "${SONATYPE_WORK}"

# ── 4. Configure nexus.rc (run as nexus user) ─────────────────────────────────
echo ">>> [4/6] Configuring Nexus to run as 'nexus' user..."
echo 'run_as_user="nexus"' > /opt/nexus/bin/nexus.rc

# ── 5. Create systemd Service ─────────────────────────────────────────────────
echo ">>> [5/6] Creating systemd service..."
tee /etc/systemd/system/nexus.service > /dev/null <<'EOF'
[Unit]
Description=Sonatype Nexus Repository Manager
After=network.target

[Service]
Type=forking
User=nexus
Group=nexus
LimitNOFILE=65536
Environment=INSTALL4J_JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ExecStart=/opt/nexus/bin/nexus start
ExecStop=/opt/nexus/bin/nexus stop
Restart=on-abort
TimeoutSec=600

[Install]
WantedBy=multi-user.target
EOF

# ── 6. Enable & Start Nexus ───────────────────────────────────────────────────
echo ">>> [6/6] Enabling and starting Nexus service..."
systemctl daemon-reload
systemctl enable --now nexus
systemctl status nexus --no-pager -l

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo "  Nexus Repository OSS ${NEXUS_VERSION} installation complete!"
echo "============================================================"
echo ""
echo "  Web UI:     http://<EC2-PUBLIC-IP>:8081"
echo "  Username:   admin"
echo "  Password:   $(cat /opt/sonatype-work/nexus3/admin.password 2>/dev/null || echo 'Run: sudo cat /opt/sonatype-work/nexus3/admin.password')"
echo ""
echo "  NOTE: Open TCP port 8081 in your EC2 Security Group inbound rules."
echo "  NOTE: Nexus takes ~2 minutes to fully start. If UI is not ready,"
echo "        wait and retry, or check logs:"
echo "        tail -f /opt/sonatype-work/nexus3/log/nexus.log"
echo "============================================================"
