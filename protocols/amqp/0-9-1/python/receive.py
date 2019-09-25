#!/usr/bin/env python
import pika

import sys

if len(sys.argv) != 5:
    print("Usage: python {} host username password virtual_host".format(sys.argv[0]))
    sys.exit(0)

host = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
virtual_host = sys.argv[4]

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
