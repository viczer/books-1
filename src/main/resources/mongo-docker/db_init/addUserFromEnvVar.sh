#!/bin/bash
# Creates the application Mongo user
# If editing, ensure file has unix style line endings - use dos2unix after editing on Windows
mongo <<EOF
use $MONGO_INITDB_DATABASE;
db.createUser({
  user:  "$MONGO_INITDB_APP_USERNAME",
  pwd: "$MONGO_INITDB_APP_PASSWORD",
  roles: [{
      role: "readWrite",
      db: "$MONGO_INITDB_DATABASE"
  }]
})
EOF
