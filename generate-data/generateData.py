import uuid
import random
import argparse
from datetime import datetime
from faker import Faker
from tqdm import tqdm
import psycopg
import bcrypt

fake = Faker()

def connect_to_db(host, user, password, database, port=5432):
    return psycopg.connect(
        host=host,
        user=user,
        password=password,
        dbname=database,
        port=port
    )

def insert_users(cursor, db, n=10000):
    print("Generating users...")
    users = []
    salt = b"$2a$10$aV5BYihLxDi8FstQA4z8XO"
    for _ in range(n):
        user_id = str(uuid.uuid4())
        email = fake.unique.email()
        username = fake.unique.user_name()
        users.append((
            user_id, datetime.now(), datetime.now(), 'ACTIVE',
            email, bcrypt.hashpw(username.encode('utf-8'), salt).decode(), 'USER',
            fake.text(max_nb_chars=100), fake.address(), fake.city(),
            fake.country(), fake.first_name(), fake.last_name(),
            fake.phone_number(), fake.postcode(), None, username
        ))
    cursor.executemany("""
                       INSERT INTO users (id, creation_timestamp, update_timestamp, active, email, password, role,
                                          about_me, address, city, country, first_name, last_name, phone_number, postal_code,
                                          profile_picture, username)
                       VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                       """, users)
    db.commit()
    return [u[0] for u in users]

def insert_categories(cursor, db, n=30000):
    print("Generating categories...")
    categories = []
    for _ in range(n):
        categories.append((
            str(uuid.uuid4()), datetime.now(), datetime.now(),
            fake.text(max_nb_chars=50), None, fake.word()
        ))
    cursor.executemany("""
                       INSERT INTO categories (id, creation_timestamp, update_timestamp, description, image_id, name)
                       VALUES (%s, %s, %s, %s, %s, %s)
                       """, categories)
    db.commit()
    return [c[0] for c in categories]

def insert_jobs(cursor, db, category_ids, n=1000000):
    print("Generating jobs...")
    job_ids = []
    for _ in tqdm(range(n), desc="Jobs"):
        job_id = str(uuid.uuid4())
        category_id = random.choice(category_ids)
        cursor.execute("""
                       INSERT INTO jobs (id, creation_timestamp, update_timestamp, description, image_id, name, category_id)
                       VALUES (%s, %s, %s, %s, %s, %s, %s)
                       """, (
                           job_id, datetime.now(), datetime.now(), fake.text(50),
                           None, fake.job(), category_id
                       ))
        job_ids.append(job_id)
        if len(job_ids) % 10000 == 0:
            db.commit()
    db.commit()
    return job_ids

def insert_job_keys(cursor, db, job_ids, keys_per_job=3):
    print("Generating job_keys...")
    job_keys = []
    for job_id in tqdm(job_ids, desc="Job Keys"):
        for _ in range(random.randint(1, keys_per_job)):
            job_keys.append((job_id, fake.word()))
        if len(job_keys) % 10000 == 0:
            cursor.executemany("INSERT INTO job_keys (job_id, key) VALUES (%s, %s)", job_keys)
            db.commit()
            job_keys = []
    if job_keys:
        cursor.executemany("INSERT INTO job_keys (job_id, key) VALUES (%s, %s)", job_keys)
        db.commit()


def insert_adverts(cursor, db, user_ids, job_ids, n=1000000):
    print("Generating adverts...")
    advert_ids = []
    for _ in tqdm(range(n), desc="Adverts"):
        advert_id = str(uuid.uuid4())
        cursor.execute("""
                       INSERT INTO adverts (id, creation_timestamp, update_timestamp, advertiser, delivery_time,
                                            description, image_id, name, price, status, user_id, job_id)
                       VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                       """, (
                           advert_id, datetime.now(), datetime.now(),
                           random.choice(['EMPLOYEE', 'CUSTOMER']), random.randint(1, 30),
                           fake.text(100), None, fake.catch_phrase(), random.randint(50, 1000),
                           random.choice(['OPEN', 'CLOSED', 'CANCELLED', 'ASSIGNED', 'REVIEWED']),
                           random.choice(user_ids), random.choice(job_ids)
                       ))
        advert_ids.append(advert_id)
        if len(advert_ids) % 10000 == 0:
            db.commit()
    db.commit()
    return advert_ids

def insert_offers(cursor, db, user_ids, advert_ids, n=500000):
    print("Generating offers...")
    offer_ids = []
    for _ in tqdm(range(n), desc="Offers"):
        offer_id = str(uuid.uuid4())
        cursor.execute("""
                       INSERT INTO offers (id, creation_timestamp, update_timestamp, offered_price, status, user_id, advert_id)
                       VALUES (%s, %s, %s, %s, %s, %s, %s)
                       """, (
                           offer_id, datetime.now(), datetime.now(),
                           random.randint(50, 1000), random.choice(['OPEN', 'CLOSED', 'ACCEPTED', 'REJECTED']),
                           random.choice(user_ids), random.choice(advert_ids)
                       ))
        offer_ids.append(offer_id)
        if len(offer_ids) % 10000 == 0:
            db.commit()
    db.commit()
    return offer_ids

def insert_notifications(cursor, db, offer_ids, user_ids, n=500000):
    print("Generating notifications...")
    for _ in tqdm(range(n), desc="Notifications"):
        cursor.execute("""
                       INSERT INTO notifications (id, creation_timestamp, message, offer_id, user_id)
                       VALUES (%s, %s, %s, %s, %s)
                       """, (
                           str(uuid.uuid4()), datetime.now(),
                           fake.sentence(nb_words=8),
                           random.choice(offer_ids), random.choice(user_ids)
                       ))
        if _ % 10000 == 0:
            db.commit()
    db.commit()

def main():
    parser = argparse.ArgumentParser(description="MySQL Test Data Generator")
    parser.add_argument("--host", default="localhost", help="MySQL host")
    parser.add_argument("--port", default=5432, help="PostgreSQL port")
    parser.add_argument("--user", default="postgres", help="MySQL user")
    parser.add_argument("--password", default="55", help="MySQL password")
    parser.add_argument("--database", default="microservice", help="MySQL database name")
    args = parser.parse_args()

    db = connect_to_db(args.host, args.user, args.password, args.database)
    cursor = db.cursor()

    user_ids = insert_users(cursor, db, 100)
    category_ids = insert_categories(cursor, db, 300)
    job_ids = insert_jobs(cursor, db, category_ids, 1000)
    insert_job_keys(cursor, db, job_ids, keys_per_job=5)
    advert_ids = insert_adverts(cursor, db, user_ids, job_ids, 1000)
    offer_ids = insert_offers(cursor, db, user_ids, advert_ids, 100)
    insert_notifications(cursor, db, offer_ids, user_ids, 100)

    print("✅ 数据生成完成！")

if __name__ == "__main__":
    main()
