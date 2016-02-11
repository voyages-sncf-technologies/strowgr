#!/usr/bin/env sh
#haproxy -f /conf/haproxy.cfg &

#mkdir /HOME/hapadm/OCE/logs/OCEREC1 -p
/haaasd --config /haaasd.conf -ip $(hostname -i)
#/haaasd --config /haaas.conf -ip $(ip addr show dev eth0|grep -o -e "[0-9][0-9]*.[0-9][0-9]*.[0-9][0-9]*.[0-9][0-9]*")