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


def callback(ch, method, properties, body):
    print(" [x] Received %r" % body)


channel.basic_consume(
    queue='hello', on_message_callback=callback, auto_ack=True)

print(' [*] Waiting for messages. To exit press CTRL+C')
channel.start_consuming()
