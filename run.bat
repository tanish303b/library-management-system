@echo off
rem Compiles the project and starts the application.
if not exist db.properties (
    echo db.properties not found. Copy db.properties.example to db.properties and set your MySQL password.
    exit /b 1
)
javac -d out -sourcepath src src\com\library\Main.java
if errorlevel 1 exit /b 1
java -cp "out;lib\*" com.library.Main
