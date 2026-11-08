# -*- coding: utf-8 -*-
"""把本机 uploads/avatars/ 里已上传的历史头像补录进 demon_avatar_file（可重复跑）。

用途：
  1) 首次启用「图片入库」时，把旧图片一次性收进库，之后 demon_db.sql 全量快照
     就自带图片，换新电脑导入快照后头像不再裂开；
  2) 从别的电脑拷来一批图片文件、但库里还没记录时，跑一次补录。

运行：MySQL 3306 已启动时 `python migrate_avatars.py`
脚本只做「新增 / 覆盖同名路径」，不会删除库里任何记录，因此重复执行安全。
"""
import base64
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent
UPLOAD = ROOT / "uploads" / "avatars"
DB = "C:/Users/czh10/mysql84/server/bin/mysql.exe"
PROPS = ROOT / "src/main/resources/application.properties"
DB_PWD = re.search(r"^spring\.datasource\.password=(.*)$",
                   PROPS.read_text(encoding="utf-8"), re.M).group(1).strip()

MIME = {
    ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png",
    ".gif": "image/gif", ".webp": "image/webp", ".bmp": "image/bmp",
}

CREATE = """
CREATE TABLE IF NOT EXISTS demon_avatar_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  file_path VARCHAR(255) NOT NULL,
  mime VARCHAR(64) NOT NULL DEFAULT 'image/jpeg',
  img_base64 LONGTEXT NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_avatar_path (file_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
"""


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "''")


def run_sql(text):
    """把（可能很大的）SQL 通过 stdin 喂给 mysql，避免命令行长度上限。"""
    p = subprocess.run(
        [DB, "-uroot", "-p" + DB_PWD, "-h127.0.0.1", "-P3306",
         "--default-character-set=utf8mb4", "demon_db"],
        input=text.encode("utf-8"), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    err = p.stderr.decode("utf-8", "replace")
    err = "\n".join(l for l in err.splitlines() if "Using a password" not in l)
    if p.returncode != 0:
        print("SQL 执行失败：", err.strip())
        sys.exit(1)
    return p.stdout.decode("utf-8", "replace")


def main():
    if not UPLOAD.is_dir():
        print("uploads/avatars/ 不存在，无需迁移（库里的图片仍可正常取用）。")
        run_sql(CREATE)
        return
    files = [f for f in sorted(UPLOAD.iterdir())
             if f.is_file() and f.suffix.lower() in MIME]
    print("扫描到 %d 张图片：%s" % (len(files), UPLOAD))

    statements = ["SET NAMES utf8mb4;", CREATE]
    for f in files:
        rel = "/uploads/avatars/" + f.name
        mime = MIME[f.suffix.lower()]
        data = base64.b64encode(f.read_bytes()).decode("ascii")
        statements.append(
            "INSERT INTO demon_avatar_file (file_path, mime, img_base64, created_at) "
            "VALUES ('%s', '%s', '%s', NOW()) "
            "ON DUPLICATE KEY UPDATE mime=VALUES(mime), img_base64=VALUES(img_base64);"
            % (esc(rel), mime, data))
        print("  + %-46s %7d B  -> base64 %d" % (f.name, f.stat().st_size, len(data)))

    run_sql("\n".join(statements))
    out = run_sql("SELECT COUNT(*) FROM demon_avatar_file;")
    print("完成。demon_avatar_file 现有 %s 条记录。" % out.strip().splitlines()[-1])


if __name__ == "__main__":
    main()
