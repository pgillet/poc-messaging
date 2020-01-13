#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

from __future__ import print_function, unicode_literals
import optparse
import time

from proton import Message
from proton._reactor import AtLeastOnce
from proton.handlers import MessagingHandler
from proton.reactor import Container
from proton import SSLDomain

import env

import os

class Send(MessagingHandler):
    def __init__(self, urls, address, messages):
        super(Send, self).__init__()
        self.urls = urls
        self.address = address
        self.sent = 0
        self.confirmed = 0
        self.total = messages
        self.start = 0

    def on_start(self, event):
        ssl_domain = SSLDomain(mode=SSLDomain.MODE_CLIENT)
        ssl_domain.set_trusted_ca_db(env.certificate_db)
        ssl_domain.set_credentials(env.cert_file, env.key_file, env.password)
        conn = event.container.connect(urls=self.urls, ssl_domain=ssl_domain) #, user=env.username, password=env.password)
        # at-least-once delivery semantics for message delivery
        event.container.create_sender(conn, self.address, options=AtLeastOnce())
        self.start = time.time()

    def on_sendable(self, event):
        while event.sender.credit and self.sent < self.total:
            msg = Message(id=(self.sent+1), body=os.urandom(env.message_size))
            msg._set_creation_time(time.time()) # not sure about that
            event.sender.send(msg)
            self.sent += 1

            # Print info
            if self.sent % env.print_info_modulus == 0:
                duration = time.time() - self.start
                average = self.sent / duration
                print(f'Average: {average:.2f} msg/s (Sent {self.sent} messages in {duration:.2f}s')

            if env.throttle > 0:
                time.sleep(env.throttle)

    def on_accepted(self, event):
        self.confirmed += 1
        if self.confirmed == self.total:
            print("all messages confirmed")
            event.connection.close()

    def on_disconnected(self, event):
        self.sent = self.confirmed

parser = optparse.OptionParser(usage="usage: %prog [options]",
                               description="Send messages to the supplied address.")
parser.add_option("-u", "--urls", default=env.server_addr,
                  help="list of URL strings of process to try to connect to. Ex: [host1:5672, host2:5672, host2:5672]")
parser.add_option("-a", "--address", default=env.address,
                  help="address to which messages are sent (default %default)")
parser.add_option("-m", "--messages", type="int", default=env.num_messages,
                  help="number of messages to send (default %default)")
opts, args = parser.parse_args()

# From command line string option
if isinstance(opts.urls, str):
    opts.urls = opts.urls.strip('[]').split(', ')

try:
    Container(Send(opts.urls, opts.address, opts.messages)).run()
except KeyboardInterrupt: pass
