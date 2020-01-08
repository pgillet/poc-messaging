# ActiveMQ Artemis
# server_addr = '35.189.241.214:5672/examples'

# Failover with a list of URL strings of process to try to connect to.
#server_addr = ["amqps://35.241.200.129:5671", "amqps://35.241.250.148:5671", "amqps://34.76.223.238:5671"]

# RabbitMQ
server_addr = ["amqps://34.77.53.43:5671", "amqps://35.195.128.110:5671", "amqps://34.76.93.87:5671"]

# Destination name
address = 'jms.queue.PerfQueue'

# Interval between the send of two messages in seconds
throttle = -1

# Message size in bytes (Ex: 1000 = 1 kb, 1024 = 1 kib)
message_size = 15000

# Auth
username = 'meteofr'
password = 'meteofr'

# TLS/SSL
certificate_db = '/home/pascal/Workbench/Projects/MeteoFrance/tls-gen/basic/tls/ca_certificate.pem'
cert_file = '/home/pascal/Workbench/Projects/MeteoFrance/tls-gen/basic/tls/client_certificate.pem'
key_file = '/home/pascal/Workbench/Projects/MeteoFrance/tls-gen/basic/tls/client_key.pem'
password = 'bunnies'