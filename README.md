# FASTA Encoding Project

This project aims to provide a system for processing biological sequence data, specifically FASTA formats, and storing them in a MongoDB instance. It includes a Java program for FASTA transcoding and a Python script for uploading files to MongoDB GridFS.

## 1. Building and Running the Java Program

The Java program (`FastaTranscoder`) is built using Gradle.

### Build
To build the executable JAR (also known as a "fat JAR" or "uber JAR") that contains all dependencies, run the following command:

```bash
./gradlew shadowJar
```

This will create a JAR file in the `build/libs/` directory, typically named `fasta_encoding-0.1-all.jar`.

### Run
To run the Java program, execute the generated JAR file using the `java -jar` command:

```bash
java -jar build/libs/fasta_encoding-0.1-all.jar encode input.fasta output.bin
```

```bash
java -jar build/libs/fasta_encoding-0.1-all.jar decode input.bin output.fasta
```


## 2. Setting Up and Running the Python Script for File Uploads

The Python script (`upload_file.py`) is used to upload GFF3 files to MongoDB GridFS.

### Setup
1. **Install Dependencies**: Ensure you have Python 3 installed. Install the required Python packages using pip inside a virtual environment:

    Setup the environment:
    ```bash
    python3 -m venv pyenv && . pyenv/bin/activate
    ```
    Install the dependencies:
    ```bash
    python -m pip install -r requirements.txt
    ```
2. **Environment Variables (Optional)**: The script can read MongoDB connection details from environment variables. You can create a `.env` file in the same directory as `upload_file.py` with the following (or similar) content:
    ```
    MONGO_URI="mongodb://localhost:27017/"
    MONGO_DB_NAME="gff3service"
    MONGO_GRIDFS_BUCKET="gff3"
    MONGO_METADATA_COLLECTION="gff3_metadata"
    ```
    Alternatively, you can pass these values as command-line arguments.

### Run
To upload a file, run the script with the path to your file:

```bash
python upload_file.py <path_to_your_gff3_file>
```

Example:
```bash
python upload_file.py data/example.gff3
```

You can also specify MongoDB connection details via command-line arguments:
```bash
python upload_file.py data/example.gff3 --uri "mongodb://localhost:27017/" --db "my_gff3_db" --bucket "my_bucket" --metadata-coll "my_metadata"
```

## 3. MongoDB Instance Access and File Size Check

The project uses a MongoDB instance managed by `docker-compose`.

### Login to MongoDB
To start the MongoDB container, ensure Docker is running and execute:

```bash
docker-compose up -d
```

You can access the MongoDB instance on `localhost:27017`. The credentials configured in `docker-compose.yml` are:
- **Username**: `user`
- **Password**: `password`

You can connect using a MongoDB client (e.g., `mongosh` or MongoDB Compass) with these credentials.

Using `mongosh` from your terminal:
```bash
mongosh "mongodb://user:password@localhost:27017/admin"
```
Once connected, you can switch to the `gff3service` database (or your specified database):
```javascript
use gff3service
```

### Find the Size of Uploaded Files

To find the size of the files, use the `get_file_size.py` script with the file's ID. 

Example:
```bash
python get_file_size.py <file_id>
```

Replace `<file_id>` with the actual ID of the file you want to check. For example:
```bash
python get_file_size.py 651e04a4f8e0a1b2c3d4e5f6
```

This script will provide an estimated storage size for the specified GridFS file.

