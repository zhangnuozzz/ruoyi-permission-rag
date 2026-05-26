'use strict'

const os = require('os')
const networkInterfaces = os.networkInterfaces

os.networkInterfaces = function patchedNetworkInterfaces() {
  try {
    return networkInterfaces.call(os)
  } catch (error) {
    if (error && error.syscall === 'uv_interface_addresses') {
      return {
        lo: [
          {
            address: '127.0.0.1',
            netmask: '255.0.0.0',
            family: 'IPv4',
            mac: '00:00:00:00:00:00',
            internal: true,
            cidr: '127.0.0.1/8'
          }
        ]
      }
    }
    throw error
  }
}
