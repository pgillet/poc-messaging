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
from proton import Message, Url
from proton.handlers import MessagingHandler
from proton.reactor import Container, DynamicNodeProperties

import env

class Client(MessagingHandler):
    def __init__(self, url, requests):
        super(Client, self).__init__()
        self.url = url
        self.requests = requests

    def on_start(self, event):
        self.sender = event.container.create_sender(self.url)
        # As well as sending requests, we need to be able to get back the responses. We create a receiver for that.
        # With Artemis, we do not need to specify an address, and we can set the dynamic option which tells the broker
        # we are connected to create a temporary address over which we can receive our responses.
        # But RabbitMQ does not support dynamic sources, and we end up using the same address (the same queue) for
        # requests and responses

        # self.receiver = event.container.create_receiver(self.sender.connection, None, dynamic=True)
        self.receiver = event.container.create_receiver(self.sender.connection, self.url.path, dynamic=False)

    def next_request(self):
        if self.receiver.remote_source.address:
            req = Message(reply_to=self.receiver.remote_source.address, body=self.requests[0])
            self.sender.send(req)

    def on_link_opened(self, event):
        """
        We need to use the address allocated by the broker as the reply_to address of our requests, so we can’t send them
        until the broker has confirmed our receiving link has been set up (at which point we will have our allocated address).
        To do that, we add an on_link_opened() method to our handler class, and if the link associated with event is the
        receiver, we use that as the trigger to send our first request.
        """
        if event.receiver == self.receiver:
            self.next_request()

    def on_message(self, event):
        print("%s => %s" % (self.requests.pop(0), event.message.body))
        if self.requests:
            self.next_request()
        else:
            event.connection.close()

REQUESTS= ["Twas brillig, and the slithy toves",
           "Did gire and gymble in the wabe.",
           "All mimsy were the borogroves,",
           "And the mome raths outgrabe."]

parser = optparse.OptionParser(usage="usage: %prog [options]",
                               description="Send requests to the supplied address and print responses.")
parser.add_option("-a", "--address", default=env.server_addr,
                  help="address to which messages are sent (default %default)")
opts, args = parser.parse_args()

url = Url(opts.address)

Container(Client(url, args or REQUESTS)).run()

