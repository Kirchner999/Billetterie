@echo off
git add .
git commit -m "Batch commit"
git pull origin main --rebase
git push -u origin main
pause
