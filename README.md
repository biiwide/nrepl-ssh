# nrepl-ssh

An nREPL over SSH transport

## Usage

Loading the namespace 'biiwide.nrepl-ssh.core will register the URI schemes below with 'clojure.tools.nrepl/url-connect multimethod.
  *  ssh:  nREPL bencoded transport over SSH
  *  nrepl+ssh:  same as above
  *  telnet+ssh:  nREPL tty transport over SSH

Sample URIs:
  *  nrepl+ssh://hostname
  *  nrepl+ssh://username@hostname
  *  nrepl+ssh://username@hostname:transport-port
  *  nrepl+ssh://username@hostname:transport-port?ssh-port=2222&transport-host=other.host.local

Defaults:
  *  transport: nrepl (bencode)
  *  username: (System/getProperty "user.name")
  *  ssh-port: 22
  *  transport-host: localhost
  *  transport-port: 7888

## License

Copyright © 2015 Ted Cushman

Distributed under the Eclipse Public License either version 1.0.
