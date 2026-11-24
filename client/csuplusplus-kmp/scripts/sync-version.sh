#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
VERSION_FILE="$ROOT_DIR/version.properties"
APP_VERSION_KT="$ROOT_DIR/shared/src/AppVersion.kt"
ANDROID_MODULE_YAML="$ROOT_DIR/androidApp/module.yaml"
IOS_PBXPROJ="$ROOT_DIR/iosApp/module.xcodeproj/project.pbxproj"
DESKTOP_RELEASE_MD="$ROOT_DIR/desktopApp/RELEASE.md"

usage() {
    cat <<EOF
用法:
  ./scripts/sync-version.sh                    根据 version.properties 同步各端版本号
  ./scripts/sync-version.sh <buildCode> <name> 更新 version.properties 并同步各端版本号
  ./scripts/sync-version.sh --check            校验各端配置是否与 version.properties 一致
EOF
}

validate_inputs() {
    code="$1"
    name="$2"
    case "$code" in
        ''|*[!0-9]*|0*)
            echo "错误: buildCode 必须是正整数，当前为: '$code'" >&2
            exit 1
            ;;
    esac
    if [ -z "$name" ]; then
        echo "错误: buildName 不能为空" >&2
        exit 1
    fi
    case "$name" in
        *\"*|*\;*|*$'\n'*)
            echo "错误: buildName 包含非法字符: '$name'" >&2
            exit 1
            ;;
    esac
}

read_prop() {
    key="$1"
    awk -F= -v k="$key" '
        /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
        $1 ~ "^[[:space:]]*" k "[[:space:]]*$" {
            val = substr($0, index($0, "=") + 1)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", val)
            gsub(/\r$/, "", val)
            print val
            exit
        }
    ' "$VERSION_FILE"
}

check_only=0
if [ "$#" -eq 1 ] && [ "$1" = "--check" ]; then
    check_only=1
elif [ "$#" -eq 1 ] && { [ "$1" = "-h" ] || [ "$1" = "--help" ]; }; then
    usage
    exit 0
elif [ "$#" -eq 2 ]; then
    validate_inputs "$1" "$2"
    cat > "$VERSION_FILE" <<EOF
# 统一管理全平台应用版本号与版本名；修改后运行 ./scripts/sync-version.sh 同步到各端配置
buildCode=$1
buildName=$2
EOF
elif [ "$#" -ne 0 ]; then
    usage >&2
    exit 1
fi

if [ ! -f "$VERSION_FILE" ]; then
    echo "错误: 未找到 $VERSION_FILE" >&2
    exit 1
fi

BUILD_CODE=$(read_prop "buildCode")
BUILD_NAME=$(read_prop "buildName")
validate_inputs "$BUILD_CODE" "$BUILD_NAME"

if [ "$check_only" -eq 1 ]; then
    status=0

    kt_code=$(sed -n 's/^const val DEFAULT_BUILD_CODE: Long = \([0-9][0-9]*\)L$/\1/p' "$APP_VERSION_KT" | head -n 1)
    kt_name=$(sed -n 's/^const val DEFAULT_BUILD_NAME: String = "\(.*\)"$/\1/p' "$APP_VERSION_KT" | head -n 1)
    if [ "$kt_code" != "$BUILD_CODE" ] || [ "$kt_name" != "$BUILD_NAME" ]; then
        echo "不一致: $APP_VERSION_KT (buildCode=$kt_code, buildName=$kt_name)" >&2
        status=1
    fi

    android_code=$(sed -n 's/^[[:space:]]*versionCode:[[:space:]]*\([0-9][0-9]*\)[[:space:]]*$/\1/p' "$ANDROID_MODULE_YAML" | head -n 1)
    android_name=$(sed -n 's/^[[:space:]]*versionName:[[:space:]]*"\(.*\)"[[:space:]]*$/\1/p' "$ANDROID_MODULE_YAML" | head -n 1)
    if [ "$android_code" != "$BUILD_CODE" ] || [ "$android_name" != "$BUILD_NAME" ]; then
        echo "不一致: $ANDROID_MODULE_YAML (versionCode=$android_code, versionName=$android_name)" >&2
        status=1
    fi

    ios_codes=$(sed -n 's/^[[:space:]]*CURRENT_PROJECT_VERSION = \(.*\);[[:space:]]*$/\1/p' "$IOS_PBXPROJ" | sort -u | tr '\n' ',' | sed 's/,$//')
    ios_names=$(sed -n 's/^[[:space:]]*MARKETING_VERSION = \(.*\);[[:space:]]*$/\1/p' "$IOS_PBXPROJ" | sort -u | tr '\n' ',' | sed 's/,$//')
    if [ "$ios_codes" != "$BUILD_CODE" ] || [ "$ios_names" != "$BUILD_NAME" ]; then
        echo "不一致: $IOS_PBXPROJ (CURRENT_PROJECT_VERSION=$ios_codes, MARKETING_VERSION=$ios_names)" >&2
        status=1
    fi

    if [ "$status" -ne 0 ]; then
        echo "期望值 (version.properties): buildCode=$BUILD_CODE, buildName=$BUILD_NAME" >&2
        echo "请运行 ./scripts/sync-version.sh 同步。" >&2
        exit 1
    fi

    echo "版本配置一致: buildCode=$BUILD_CODE, buildName=$BUILD_NAME"
    exit 0
fi

cat > "$APP_VERSION_KT" <<EOF
// 由 scripts/sync-version.sh 根据 version.properties 自动同步，请勿手动修改。
package ink.qingshuige.csuplusplus

const val DEFAULT_BUILD_CODE: Long = ${BUILD_CODE}L
const val DEFAULT_BUILD_NAME: String = "$BUILD_NAME"
EOF

tmp_file=$(mktemp)
trap 'rm -f "$tmp_file"' EXIT

awk -v code="$BUILD_CODE" -v name="$BUILD_NAME" '
    /^[[:space:]]*versionCode:/ {
        sub(/versionCode:.*/, "versionCode: " code)
    }
    /^[[:space:]]*versionName:/ {
        sub(/versionName:.*/, "versionName: \"" name "\"")
    }
    { print }
' "$ANDROID_MODULE_YAML" > "$tmp_file"
mv "$tmp_file" "$ANDROID_MODULE_YAML"

tmp_file=$(mktemp)
awk -v code="$BUILD_CODE" -v name="$BUILD_NAME" '
    /CURRENT_PROJECT_VERSION = / {
        sub(/CURRENT_PROJECT_VERSION = .*;/, "CURRENT_PROJECT_VERSION = " code ";")
    }
    /MARKETING_VERSION = / {
        sub(/MARKETING_VERSION = .*;/, "MARKETING_VERSION = " name ";")
    }
    { print }
' "$IOS_PBXPROJ" > "$tmp_file"
mv "$tmp_file" "$IOS_PBXPROJ"

if [ -f "$DESKTOP_RELEASE_MD" ]; then
    tmp_file=$(mktemp)
    awk -v name="$BUILD_NAME" '
        /--app-version "/ {
            sub(/--app-version "[^"]*"/, "--app-version \"" name "\"")
        }
        { print }
    ' "$DESKTOP_RELEASE_MD" > "$tmp_file"
    mv "$tmp_file" "$DESKTOP_RELEASE_MD"
fi

echo "已同步版本到全平台: buildCode=$BUILD_CODE, buildName=$BUILD_NAME"
