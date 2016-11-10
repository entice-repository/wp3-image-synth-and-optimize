#!/bin/sh

#	Copyright 2009-2016 Gabor Kecskemeti, Akos Hajnal, University of Westminster, MTA SZTAKI
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

IP=$1
PORT=$2
INTIP=$3
PRIVKEY=$4
LOGIN=root
if [ $# -gt 4 ] ; then LOGIN=$5 ; fi
SSHOPTS="-o ForwardX11=no -o ServerAliveInterval=5 -o ServerAliveCountMax=5 -o BatchMode=yes -o ConnectTimeout=5 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $PRIVKEY"
ssh $SSHOPTS $LOGIN@$IP reboot