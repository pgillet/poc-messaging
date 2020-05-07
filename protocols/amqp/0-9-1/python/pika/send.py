#!/usr/bin/env python
import pika
import sys

try:
    host, username, password, virtual_host = sys.argv[1:5]
except ValueError:
    sys.exit("Usage: python {} host username password virtual_host".format(sys.argv[0]))

credentials = pika.credentials.PlainCredentials(username, password)

connection = pika.BlockingConnection(
    pika.ConnectionParameters(host=host, credentials=credentials, virtual_host=virtual_host))
channel = connection.channel()

channel.queue_declare(queue='hello')

channel.basic_publish(exchange='', routing_key='hello', body='Hello World!')
print(" [x] Sent 'Hello World!'")
connection.close()
