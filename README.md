# Stegjava: Image Steganography Tool

A web tool, written in Java, that hides a secret text message inside a PNG image using LSB (least significant bit) steganography. The image looks the same to the human eye, but it carries the message.

## How it works

1. Every pixel has three colour values (red, green, blue), and each is a number from 0 to 255.
2. The tool changes only the **last bit** of each value, which is too small a change to see.
3. The first 32 bits store the **message length**, so the decoder knows how many bits to read.
4. The message bytes (UTF-8) are stored in the bits after that.

Capacity = width × height × 3 bits. Messages that don't fit are rejected with a clear error.

## Why PNG only?

PNG is lossless, so the hidden bits survive. JPG compression would destroy them.

## Features

- Hide a text message inside a PNG image
- Capacity check, so oversized messages are rejected
- Length header and validation in the decoder, so images with no hidden message are detected
- Web interface (HTML, CSS, JavaScript) with image preview and download

## Tech stack

Java 17, HTML, CSS, JavaScript

## Project structure

```
src/
  LSBSteganography.java   core encode / decode logic
  StegServer.java         small HTTP server (serves the page, handles /encode)
  SetgJavaServer.java     servlet version for Apache Tomcat
stegjava/
  index.html, style.css, app.js   frontend
```

## How to run

Requires JDK 17 or newer. From the project folder:

```
javac --release 17 -d out src\LSBSteganography.java src\StegServer.java
java -cp out src.StegServer
```

Then open http://localhost:9090 in your browser.


## Planned

- Reveal page in the web interface (the decode logic already exists in `LSBSteganography.java`)
- Password-based encryption of the message before hiding it (the password field exists in the form but is not used yet)
