# ActiveMQ Artemis
# server_addr = 'host:5672/examples'

# Failover with a list of URL strings of process to try to connect to.
#server_addr = ["amqps://host1:5671", "amqps://host2:5671", "amqps://host3:5671"]

# RabbitMQ
server_addr = ["amqps://host1:5671", "amqps://host2:5671", "amqps://host3:5671"]

# Destination name
address = '/topic/test'

# Interval between the send of two messages in seconds
throttle = 0

# Message size in bytes (Ex: 1000 = 1 kb, 1024 = 1 kib)
message_size = 15000

# Number of messages to be sent/received
num_messages = 100000

# Auth
username = 'dummy'
password = 'password'

# TLS/SSL
certificate_db = '/path/to/tls-gen/basic/tls/ca_certificate.pem'
cert_file = '/path/to/tls-gen/basic/tls/client_certificate.pem'
key_file = '/path/to/tls-gen/basic/tls/client_key.pem'
password = 'bunnies'


print_info_modulus = 10
