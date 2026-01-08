#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)
BACKEND_STATIC="$ROOT_DIR/backend/src/main/resources/static"
DIST_DIR="$ROOT_DIR/dist"
PACKAGE_DIR="$DIST_DIR/package"

echo "=========================================="
echo "Build release package"
echo "=========================================="

if ! command -v pnpm >/dev/null 2>&1; then
  echo "pnpm not found. Please install pnpm or adjust this script."
  exit 1
fi

echo "[1/6] Clean backend static resources..."
mkdir -p "$BACKEND_STATIC"
rm -rf "$BACKEND_STATIC"/*

echo "[2/6] Build frontend (web)..."
cd "$ROOT_DIR/frontend"
pnpm --filter @blog/web build

echo "[3/6] Build frontend (admin)..."
pnpm --filter @blog/admin build

echo "[4/6] Inject frontend assets into backend..."
cp -r "$ROOT_DIR/frontend/packages/web/dist/"* "$BACKEND_STATIC"
mkdir -p "$BACKEND_STATIC/admin"
cp -r "$ROOT_DIR/frontend/packages/admin/dist/"* "$BACKEND_STATIC/admin"

echo "[5/6] Build backend jar..."
cd "$ROOT_DIR/backend"
./mvnw clean package -DskipTests

echo "[6/6] Create release package..."
rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/config"

JAR_PATH=$(ls "$ROOT_DIR"/backend/target/*.jar | grep -v "original" | head -n 1 || true)
if [ -z "$JAR_PATH" ]; then
  echo "Jar not found in backend/target"
  exit 1
fi

cp "$JAR_PATH" "$PACKAGE_DIR/blog.jar"
cp "$ROOT_DIR/backend/src/main/resources/application.yml" "$PACKAGE_DIR/config/application.yml"

cat > "$PACKAGE_DIR/start.sh" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

APP_HOME=$(cd "$(dirname "$0")" && pwd)
JAR="$APP_HOME/blog.jar"
CONFIG_DIR="$APP_HOME/config"

JAVA_OPTS=${JAVA_OPTS:--Xms256m -Xmx768m}
SPRING_OPTS="--spring.config.additional-location=$CONFIG_DIR/"

exec java $JAVA_OPTS -jar "$JAR" $SPRING_OPTS
EOF
chmod +x "$PACKAGE_DIR/start.sh"

mkdir -p "$DIST_DIR"
tar -czf "$DIST_DIR/blog-release.tar.gz" -C "$PACKAGE_DIR" .

echo ""
echo "Package created: dist/blog-release.tar.gz"
echo "=========================================="
