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
from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container

import env
import time

class Send(MessagingHandler):
    def __init__(self, urls, address, messages):
        super(Send, self).__init__()
        self.urls = urls
        self.address = address
        self.sent = 0
        self.confirmed = 0
        self.total = messages

    def on_start(self, event):
        conn = event.container.connect(urls=self.urls)
        event.container.create_sender(conn, self.address)

    def on_sendable(self, event):
        while event.sender.credit and self.sent < self.total:
            msg = Message(id=(self.sent+1), body={'sequence':(self.sent+1)})
            event.sender.send(msg)
            self.sent += 1

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
parser.add_option("-a", "--address", default="examples",
                  help="address to which messages are sent (default %default)")
parser.add_option("-m", "--messages", type="int", default=100,
                  help="number of messages to send (default %default)")
opts, args = parser.parse_args()

# From command line string option
if isinstance(opts.urls, str):
    opts.urls = opts.urls.strip('[]').split(', ')

try:
    Container(Send(opts.urls, opts.address, opts.messages)).run()
except KeyboardInterrupt: pass
