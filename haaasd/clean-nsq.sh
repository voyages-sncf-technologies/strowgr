#!/bin/sh

LOOKUPD=floradora:50161

curl -X POST http://$LOOKUPD/topic/delete?topic=try_update_default-name
curl -X POST http://$LOOKUPD/topic/delete?topic=update_default-name
curl -X POST http://$LOOKUPD/topic/delete?topic=updated_default-name