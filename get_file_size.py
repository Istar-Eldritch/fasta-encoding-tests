import argparse
import os
from pymongo import MongoClient
from bson import ObjectId
from dotenv import load_dotenv

load_dotenv()

def get_gridfs_file_size(file_id_str, mongo_uri="mongodb://localhost:27017/", db_name="gff3dev", gridfs_bucket="gff3"):
    try:
        client = MongoClient(mongo_uri)
        db = client[db_name]
        gridfs_chunks_name = f"{gridfs_bucket}.chunks";
        gridfs_chunks = db[gridfs_chunks_name]
        gridfs_files = db[f"{gridfs_bucket}.files"]
        file_id = ObjectId(file_id_str)

        num_chunks = gridfs_chunks.count_documents({'files_id': file_id})
        chunk_stats = db.command("collstats", gridfs_chunks_name)
        file_size = (chunk_stats["avgObjSize"] * num_chunks) / 1024 / 1024

        print(f"Estimated storage for file ID '{file_id_str}': {file_size} Mb")

    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        if 'client' in locals() and client:
            client.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Get the estimated storage size for a GridFS file.")
    parser.add_argument("file_id", help="The ID of the GridFS file.")
    parser.add_argument("--uri", default=os.getenv("MONGO_URI", "mongodb://localhost:27017/"), help="MongoDB connection URI (default: mongodb://localhost:27017/).")
    parser.add_argument("--db", default=os.getenv("MONGO_DB_NAME", "gff3dev"), help="MongoDB database name (default: gff3dev).")
    parser.add_argument("--bucket", default=os.getenv("MONGO_GRIDFS_BUCKET", "gff3"), help="GridFS bucket name (default: gff3).")

    args = parser.parse_args()

    get_gridfs_file_size(args.file_id, args.uri, args.db, args.bucket)

