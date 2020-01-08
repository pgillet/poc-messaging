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

from __future__ import print_function
import optparse

from proton import SSLDomain
from proton.handlers import MessagingHandler
from proton.reactor import Container

import env

class Recv(MessagingHandler):
    def __init__(self, urls, address, count):
        super(Recv, self).__init__()
        self.urls = urls
        self.address = address
        self.expected = count
        self.received = 0

    def on_start(self, event):
        ssl_domain = SSLDomain(mode=SSLDomain.MODE_CLIENT)
        ssl_domain.set_trusted_ca_db(env.certificate_db)
        ssl_domain.set_credentials(env.cert_file, env.key_file, env.password)
        conn = event.container.connect(urls=self.urls, ssl_domain=ssl_domain, user=env.username, password=env.password)
        event.container.create_receiver(conn, self.address)

    def on_message(self, event):
        if event.message.id and event.message.id < self.received:
            # ignore duplicate message
            return
        if self.expected == 0 or self.received < self.expected:
            print(event.message.body)
            self.received += 1
            if self.received == self.expected:
                event.receiver.close()
                event.connection.close()

parser = optparse.OptionParser(usage="usage: %prog [options]")
parser.add_option("-u", "--urls", default=env.server_addr,
                  help="list of URL strings of process to try to connect to. Ex: [host1:5672, host2:5672, host2:5672]")
parser.add_option("-a", "--address", default=env.address,
                  help="address from which messages are received (default %default)")
parser.add_option("-m", "--messages", type="int", default=100,
                  help="number of messages to receive; 0 receives indefinitely (default %default)")
opts, args = parser.parse_args()

# From command line string option
if isinstance(opts.urls, str):
    opts.urls = opts.urls.strip('[]').split(', ')

try:
    Container(Recv(opts.urls, opts.address, opts.messages)).run()
except KeyboardInterrupt: pass



