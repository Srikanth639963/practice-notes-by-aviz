#!/bin/bash
# =============================================================================
# SonarQube Installation Script for Amazon Linux / RHEL-based Systems
# =============================================================================
# Usage: sudo bash sonarqube-install.sh
# NOTE: Replace 'StrongPassw0rd' with your own strong password before running.
# =============================================================================

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
SONAR_DB_PASSWORD="StrongPassw0rd"
SONAR_VERSION="25.11.0.114957"
SONAR_ZIP="sonarqube-${SONAR_VERSION}.zip"
SONAR_DOWNLOAD_URL="https://binaries.sonarsource.com/Distribution/sonarqube/${SONAR_ZIP}"
SONAR_INSTALL_DIR="/opt/sonarqube"
SONARQUBE_IP="172.31.11.168"
JENKINS_IP="172.31.5.24"

# ── 1. System Update & Dependencies ───────────────────────────────────────────
echo ">>> [1/9] Updating system and installing dependencies..."
dnf update -y
dnf install -y \
  java-21-amazon-corretto-devel \
  wget \
  unzip

# ── 2. Install & Start PostgreSQL 15 ─────────────────────────────────────────
echo ">>> [2/9] Installing PostgreSQL 15..."
dnf install -y postgresql15 postgresql15-server
/usr/bin/postgresql-setup --initdb
systemctl enable --now postgresql
systemctl status postgresql --no-pager

# ── 3. Kernel & System Limits ─────────────────────────────────────────────────
echo ">>> [3/9] Applying kernel parameters and system limits..."

cat <<EOF >> /etc/sysctl.d/99-sonarqube.conf
vm.max_map_count=262144
fs.file-max=65536
EOF
sysctl --system

cat <<EOF >> /etc/security/limits.conf
sonar   -   nofile  65536
sonar   -   nproc   4096
EOF

# ── 4. Create PostgreSQL User & Database ──────────────────────────────────────
echo ">>> [4/9] Creating PostgreSQL user and database..."
sudo -u postgres psql -c "CREATE USER sonar WITH PASSWORD '${SONAR_DB_PASSWORD}';"
sudo -u postgres psql -c "CREATE DATABASE sonarqube OWNER sonar;"

# ── 5. Configure pg_hba.conf (switch to MD5 auth) ────────────────────────────
echo ">>> [5/9] Configuring PostgreSQL authentication (MD5)..."
cp /var/lib/pgsql/data/pg_hba.conf /var/lib/pgsql/data/pg_hba.conf.bak

cat <<'EOF' > /var/lib/pgsql/data/pg_hba.conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD
# "local" is for Unix domain socket connections only
local   all             all                                     md5
# IPv4 local connections:
host    all             all             127.0.0.1/32            md5
# IPv6 local connections:
host    all             all             ::1/128                 md5
# Allow replication connections from localhost, by a user with the
# replication privilege.
local   replication     all                                     md5
host    replication     all             127.0.0.1/32            md5
host    replication     all             ::1/128                 md5
EOF

systemctl restart postgresql
systemctl status postgresql --no-pager

# ── 6. Download & Install SonarQube ──────────────────────────────────────────
echo ">>> [6/9] Downloading and installing SonarQube ${SONAR_VERSION}..."
cd /opt
wget "${SONAR_DOWNLOAD_URL}"
unzip "${SONAR_ZIP}"
mv "sonarqube-${SONAR_VERSION}" sonarqube

# Create dedicated system user and set ownership
useradd -r -s /bin/false sonar
chown -R sonar:sonar "${SONAR_INSTALL_DIR}"

# ── 7. Configure SonarQube Properties ────────────────────────────────────────
echo ">>> [7/9] Writing sonar.properties..."

cat <<EOF >> "${SONAR_INSTALL_DIR}/conf/sonar.properties"

# Database config
sonar.jdbc.url=jdbc:postgresql://localhost/sonarqube
sonar.jdbc.username=sonar
sonar.jdbc.password=${SONAR_DB_PASSWORD}
# Listen on all interfaces (useful on EC2)
sonar.web.host=0.0.0.0
sonar.web.port=9000
EOF

# ── 8. Create systemd Service ─────────────────────────────────────────────────
echo ">>> [8/9] Creating systemd service for SonarQube..."

cat <<'EOF' > /etc/systemd/system/sonarqube.service
[Unit]
Description=SonarQube service
After=network.target postgresql.service

[Service]
Type=forking
User=sonar
Group=sonar
ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop
Restart=on-failure
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now sonarqube
systemctl status sonarqube --no-pager

# ── 9. Done ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo "  SonarQube installation complete!"
echo "============================================================"
echo ""
echo "  Web UI:       http://${SONARQUBE_IP}:9000"
echo "  Default cred: admin / admin  (change on first login)"
echo ""
echo "  Next steps (manual):"
echo "  1. Log in and create a local project: portalproject"
echo "  2. Generate an admin token:"
echo "     Administration → Users → Administrator → Generate Token"
echo "  3. In Jenkins, install: SonarQube Scanner plugin"
echo "  4. In Jenkins → Manage Jenkins → Configure System:"
echo "     - Add SonarQube Server (name: sonarqube)"
echo "     - URL: http://${SONARQUBE_IP}:9000"
echo "     - Add credential using the token above (name: sonarqube)"
echo "  5. In SonarQube → Administration → Configuration → Webhooks:"
echo "     - Name: jenkins"
echo "     - URL:  http://${JENKINS_IP}:8080/sonarqube-webhook/"
echo "============================================================"
