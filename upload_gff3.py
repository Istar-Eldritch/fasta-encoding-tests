import argparse
import os
import hashlib
from pymongo import MongoClient
from gridfs import GridFS
from bson.objectid import ObjectId
from dotenv import load_dotenv
import os

load_dotenv()

def upload_gff3_to_mongodb(file_path, mongo_uri="mongodb://localhost:27017/", db_name="gff3service", gridfs_bucket="gff3", metadata_collection="gff3_metadata"):
    """
    Uploads a GFF3 file to MongoDB GridFS and its metadata to a separate collection.

    Args:
        file_path (str): The path to the GFF3 file to upload.
        mongo_uri (str): MongoDB connection URI.
        db_name (str): The name of the MongoDB database.
        gridfs_bucket (str): The name of the GridFS bucket.
        metadata_collection (str): The name of the metadata collection.
    """
    try:
        client = MongoClient(mongo_uri)
        db = client[db_name]
        fs = GridFS(db, collection=gridfs_bucket)

        # 1. Store file in GridFS
        with open(file_path, 'rb') as f:
            file_id = fs.put(f, filename=os.path.basename(file_path), contentType='text/plain')
        print(f"File '{os.path.basename(file_path)}' uploaded to GridFS with ID: {file_id}")

        # 2. Calculate MD5 checksum and file size
        file_size = os.path.getsize(file_path)
        with open(file_path, 'rb') as f:
            md5_hash = hashlib.md5(f.read()).hexdigest()

        # 3. Store metadata in a separate collection
        metadata_coll = db[metadata_collection]
        object_id = os.path.basename(file_path).split('.')[0] # Simple object_id from filename for example

        metadata = {
            "_id": object_id.upper(),
            "file_id": file_id,
            "md5": md5_hash,
            "file_size": file_size,
            "filename": os.path.basename(file_path) # Store filename for easier lookup
        }
        metadata_coll.insert_one(metadata)
        print(f"Metadata for '{os.path.basename(file_path)}' uploaded to '{metadata_collection}' collection.")

    except FileNotFoundError:
        print(f"Error: File not found at '{file_path}'")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        if 'client' in locals() and client:
            client.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Upload a GFF3 file to MongoDB GridFS and its metadata.")
    parser.add_argument("file", help="Path to the GFF3 file to upload.")
    parser.add_argument("--uri", default=os.getenv("MONGO_URI", "mongodb://localhost:27017/"), help="MongoDB connection URI (default: mongodb://localhost:27017/).")
    parser.add_argument("--db", default=os.getenv("MONGO_DB_NAME", "gff3dev"), help="MongoDB database name (default: gff3service).")
    parser.add_argument("--bucket", default=os.getenv("MONGO_GRIDFS_BUCKET", "gff3"), help="GridFS bucket name (default: gff3).")
    parser.add_argument("--metadata-coll", default=os.getenv("MONGO_METADATA_COLLECTION", "gff3_metadata"), help="Metadata collection name (default: gff3_metadata).")

    args = parser.parse_args()

    upload_gff3_to_mongodb(args.file, args.uri, args.db, args.bucket, args.metadata_coll)

