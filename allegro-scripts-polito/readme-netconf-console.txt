
#Commands to get information
netconf-console2 --host=192.168.88.13 --port=830 -u root -p password --rpc=get-components.xml

#Commands for edit-config
netconf-console2 --host=192.168.88.13 --port=830 -u root -p password --db=running --edit-config=set-frequency.xml

