# Router feature inventory – DIGI Wi-Fi 7 (TP-Link XGB430v Pro, FW 0.2.0 3.2.1 Build 250808)

🇪🇸 [Versión en español](router_features.es.md)

Generated automatically from the router's real menu (`frame/menu.htm`), evaluated with the device's feature flags and the pages actually installed (`menu.cgi`).

- **U** = visible with the `user` account (*User* role). **A** = only with `admin` (*Admin* role).
- "Objects" are the data-model objects (`DEV2_*`, `ACT_*`) the page reads/writes on the router.

## Advanced menu


### Status

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `status.htm` | Status (WAN/PON, IPv4/IPv6) | `DEV2_GPON_INTF_OMCI_STATS` `DEV2_GPON_INTF_STATS` `DEV2_OPTC_GPON_CFG` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` |

### Internet / Network

| Role | Page | Function | Objects |
|---|---|---|---|
| A | `ethWan.htm` | Ethernet WAN | `DEV2_ADT_WAN` `DEV2_ETH` `DEV2_ETH_INTF` `DEV2_FIREWALL` `DEV2_FLOWCONTROL` `DEV2_INTERNET_DOMAIN` `DEV2_IQOS` `DEV2_MANAGEMENT_SERVER` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_X_TP_DEFAULTGATEWAY` |
| A | `wan.htm` | Network / WAN | `DEV2_ADT_WAN` `DEV2_DSL_CHANNEL` `DEV2_OPTC_INTF` |
| U | `dhcp.htm` | LAN / DHCP server / reservations | `DEV2_ADT_LAN` `DEV2_ARP_BIND_ENTRY` `DEV2_DHCPV4_POOL_STATICADDR` `DEV2_DHCPV4_SERVER_POOL` `DEV2_FLOWCONTROL` `DEV2_HOST_ENTRY` |
| A | `gponcfg.htm` | PON configuration (GPON/XGS-PON) | — |
| U | `ddns.htm` | Dynamic DNS | `CLOUD_DDNS` `CLOUD_DDNS_ENTRY` `DEV2_CURRENT_USER` `DEV2_DYN_DNS_CFG` `DEV2_NOIP_DNS_CFG` `DEV2_USERDEFINE_DDNS_CFG` |
| A | `route.htm` | Advanced routing | `DEV2_ADT_WAN` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` |
| A | `rip.htm` | RIP | `DEV2_ADT_WAN` `DEV2_RIP` |

### Wi-Fi

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `wirelessSettings.htm` | Per-band Wi-Fi settings (channel, width, power, standard) | `ACT_AUTO_CHAN_SELECT` `ACT_WIFI_RELOAD_MLO` `DEV2_ADT_WIFI_COMMON` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` `DEV2_WIFI_RADIO` `DEV2_XTP_GPIO_BTN` |
| U | `wps.htm` | WPS | `ACT_WIFI_GET_NEW_PIN` `ACT_WIFI_RESTORE_PIN` `DEV2_ADT_WIFI_COMMON` |
| U | `macFilter.htm` | Wi-Fi MAC filter | `DEV2_ADT_WIFI_CLIENT` `DEV2_ADT_WIFI_COMMON` `DEV2_ADT_WIFI_MACTABLE` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV_AFFSTAINFO` |
| U | `wirelessSchedule.htm` | Wi-Fi schedule | `ACT_WIFI_RELOAD_MLO` `DEV2_ADT_WIFI_SCHEDULE` |
| U | `wirelessStat.htm` | Wi-Fi statistics / clients | `ACT_WIFI_UPDATE_ALLASSOC` `DEV2_ADT_WIFI_CLIENT` `DEV2_ADT_WIFI_COMMON` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV_ASSOCDEV` |
| U | `wirelessAdv.htm` | Advanced Wi-Fi | `ACT_WIFI_UPDATE_ASSOC` `DEV2_ADT_WIFI_COMMON` `DEV2_DHCPV4_SERVER_POOL` `DEV2_X_TP_EASYMESH` |
| U | `multiSSID.htm` | Multi-SSID | `DEV2_ADT_WIFI_COMMON` `DEV2_HOUR` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` `DEV2_WIFI_SCHEDULE` |
| U | `easyMesh.htm` | EasyMesh | `ACT_FACTORY_RESET` `ACT_REBOOT` `DEV2_ADT_WIFI_COMMON` `DEV2_DEV_INFO` `DEV2_LOCAL` `DEV2_WIFI_APDEV` `DEV2_X_TP_AGENTINFO` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_ONBOARDBYSCANNING` |

### Guest network

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `wlGuestDulBandAdv.htm` | Guest network (advanced) | `DEV2_ADT_WIFI_COMMON` `DEV2_IQOS` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` |

### Telephony

| Role | Page | Function | Objects |
|---|---|---|---|
| A | `voice_advance.htm` | Advanced telephony (admin) | `DEV2_ADT_WAN` `DEV2_LOCAL` `XTP_MULTIISP_CODEC_LIST` `XTP_VOICE_CALL_FWD_LIST` `XTP_VOICESERVICE` |
| A | `voice_telebook.htm` | Phone book | — |
| U | `voice_calllog.htm` | Call log | — |
| A | `voice_digitmap.htm` | Dial plan | — |
| A | `voice_callblocks.htm` | Call blocking | `DEV2_LOCAL` |
| A | `voice_callforward.htm` | Call forwarding | — |

### Port forwarding / NAT

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `alg.htm` | ALG | `DEV2_ALG_CFG` |
| U | `virtualServer.htm` | Virtual servers (port forwarding) | `DEV2_ADT_WAN` `DEV2_IP_INTF` `DEV2_PORTMAPPING` `DEV2_ROUTER_V4FWD` |
| U | `portTrigger.htm` | Port triggering | `DEV2_ADT_WAN` `DEV2_PORTTRIGGERING` `DEV2_ROUTER_V4FWD` |
| U | `dmz.htm` | DMZ | `DEV2_ADT_LAN` `DEV2_DMZ_HOST_CFG` |
| U | `upnp.htm` | UPnP | `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` |

### USB

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `diskSettings.htm` | Disk settings | `DEV2_LOGICAL_VOLUME` `DEV2_USB_DEVICE` |
| U | `folderSharing.htm` | Shared folders | `DEV2_ADT_LAN` `DEV2_ADT_WAN` `DEV2_DLNA_FOLDER` `DEV2_DLNA_MEDIA_SERVER` `DEV2_DMZ_HOST_CFG` `DEV2_FOLDER_BROWSE` `DEV2_FOLDER_NODE` `DEV2_FTP_SERVER` `DEV2_FTP_SERVER_FOLDER` `DEV2_LOGICAL_VOLUME` `DEV2_PORTMAPPING` `DEV2_PORTTRIGGERING` `DEV2_ROUTER_V4FWD` `DEV2_SMB_SERVICE` `DEV2_SMB_SERVICE_FOLDER` `DEV2_SMB_USER_ACCESS` `DEV2_STORAGE_HTTP` `DEV2_STORAGE_HTTP_FOLDER` `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` `DEV2_USB_DEVICE` `DEV2_USER_ACCOUNT` `DEV2_USER_CFG` |
| A | `usb3g.htm` | USB 3G | `DEV2_ADT_WAN` `DEV2_SYSMODE` `DEV2_USB` |

### QoS

| Role | Page | Function | Objects |
|---|---|---|---|
| A | `iqosSettings.htm` | QoS | `DEV2_GAME_QOS_CLIENT` `DEV2_HOST_ENTRY` `DEV2_IQOS` `DEV2_IQOS_RULE` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_WIFI_APDEV` |

### Security

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `portFiltering.htm` | Port filtering / firewall | `DEV2_ADT_LAN` `DEV2_FIREWALL` `DEV2_FW_CHAIN_RULE` |
| U | `ddos.htm` | DoS protection | `DEV2_DDOS_CFG` `DEV2_DOS_HOST` `DEV2_FIREWALL` `DEV2_STAT_CFG` |
| U | `accessControl.htm` | Access control | `/cgi/info` `DEV2_FIREWALL` `DEV2_FW_CHAIN` `DEV2_FW_CHAIN_RULE` `DEV2_HOST_ENTRY` `DEV2_WIFI_ACCESSCONTROL` `DEV2_WIFI_APDEV` `DEV2_WIFI_APDEV_ASSOCDEV` `DEV2_WIFI_APDEV_ETHASSOCDEV` `DEV2_X_TP_ONEMESH_DEVICE` |
| U | `arpBind.htm` | ARP binding | `DEV2_ARP_BIND` `DEV2_ARP_BIND_ENTRY` `DEV2_ARP_ENTRY` `DEV2_DHCPV4_POOL_STATICADDR` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV` `DEV2_X_TP_ONEMESH_DEVICE` |
| U | `ipv6Firewall.htm` | IPv6 firewall | `DEV2_ADT_WAN` `DEV2_PORTMAPPING` `DEV2_ROUTER_V6FWD` |

### VPN

| Role | Page | Function | Objects |
|---|---|---|---|
| A | `openvpnServer.htm` | OpenVPN server | `DEV2_OPENVPN` |
| A | `pptpvpnServer.htm` | PPTP server | — |
| A | `ipsec.htm` | IPSec | `DEV2_IPSEC` `DEV2_IPSEC_CFG` |
| A | `vpnStatus.htm` | VPN status | — |

### Tools / System

| Role | Page | Function | Objects |
|---|---|---|---|
| U | `time.htm` | Date and time | `DEV2_ADT_WAN` `DEV2_HOUR` `DEV2_LOCAL` `DEV2_TIME` |
| U | `ledSchedule.htm` | LED schedule | `DEV2_LED_SCHEDULE_CFG` |
| U | `diagnostic.htm` | Diagnostics (ping, traceroute, nslookup) | `ACT_DIAG_WEB_INTERNETDIAG` `DEV2_ADT_WAN` `DEV2_DIAG_IPPING` `DEV2_DIAG_NSLOOKUP` `DEV2_DIAG_TOOL` `DEV2_DIAG_TRACERT` `DEV2_DSL_LINE` `DEV2_ETH_INTF` `DEV2_FAST_LINE` `DEV2_IP_INTF` `DEV2_NSLOOKUP_RESULT` `DEV2_OPTC_GPON_CFG` `DEV2_ROUTE_HOPS` `DEV2_ROUTE_HOPSV6` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_USB_INTF` |
| A | `softup.htm` | Firmware update | `/cgi/localMeshUpgrade` `ACT_DIAG_DNSDIAG` `CLOUD_SERVICE` `DEV2_CELL_INTF` `DEV2_DEV_INFO` `DEV2_DIAG_TOOL` `DEV2_HOUR` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_SYS_CFG` `DEV2_WIFI_APDEV` `DEV2_WIFI_APDEV_COMPONENT` `DEV2_WIFI_RADIO` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_UPGRADE` `DEV2_XTP_LTE` `FW_AUTO_UPGRADE` `FW_UPGRADE_INFO` `RE_FW_UPGRADE_INFO` |
| U | `backNRestore.htm` | Backup / restore | `ACT_REBOOT` `DEV2_CURRENT_USER` `DEV2_DEV_INFO` `DEV2_SYS_CFG` |
| U | `restartSchedule.htm` | Reboot (and schedule) | `DEV2_HOUR` `DEV2_REBOOT_SCHEDULE_CFG` `DEV2_TIME` |
| U | `manageCtrl.htm` | Administration (accounts, remote access, SSH/Telnet, factory reset) | `/cgi/auth` `/cgi/info` `/cgi/logout` `ACT_FACTORY_RESET` `ACT_OPTION66_RESET` `ACT_REBOOT` `CLOUD_DDNS` `DEV2_ACL_CFG` `DEV2_ADT_LAN` `DEV2_ADT_WAN` `DEV2_CURRENT_USER` `DEV2_DHCPV4_SERVER_POOL` `DEV2_DMZ_HOST_CFG` `DEV2_DYN_DNS_CFG` `DEV2_HTTP_CFG` `DEV2_NOIP_DNS_CFG` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_PAGE_OPERATIONMODE` `DEV2_PORTMAPPING` `DEV2_PORTTRIGGERING` `DEV2_PPP` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_SSH_CFG` `DEV2_SUPER_HTTP_CFG` `DEV2_TELNET_CFG` `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` `DEV2_USER_CFG` `DEV2_USERDEFINE_DDNS_CFG` `DEV2_USERS_USER` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_ISPRESTORE` |
| A | `log.htm` | System log | `DEV2_PAGE_ACCESS_PERMISSION` |
| U | `stat.htm` | Traffic statistics | `DEV2_DDOS_CFG` `DEV2_STAT_CFG` `DEV2_STAT_ENTRY` |
| U | `sessionTimeout.htm` | Session timeout | `SESSION_TIMEOUT` |

## Additional basic views

| Role | Page | Function |
|---|---|---|
| U | `networkMap.htm` | Network map / connected devices |
| A | `wanBasic.htm` | Internet (basic) |
| U | `wirelessBasic.htm` | Basic Wi-Fi (SSID, password, bands, MLO) |
| U | `wlGuestDulBandBasic.htm` | Guest network (basic) |
| U | `multiSSIDBasic.htm` | Basic multi-SSID |
| A | `voice_basic.htm` | Basic telephony (admin) |
| U | `voice_telephony.htm` | Telephony devices / DECT |
| U | `usbManage.htm` | USB / storage (SMB, FTP, DLNA) |
| A | `basic3g.htm` | Basic 3G |
| U | `parentCtrl_v2.htm` | Parental control |

## Summary

- Advanced-menu pages visible with `user`: **33**
- Additional pages only with `admin`: **18**

> The "Admin" pages (WAN/PON, routing, QoS, VPN, firmware, log, advanced telephony...) are only shown in the web UI with the `admin` account. The app hides them for `user` and they are not implemented.

## Implementation status in the app

Implemented (user role, 23 screens): Summary, Devices, main Wi-Fi + MLO, Radio, advanced Wi-Fi + band steering, WPS, Guest network, Multi-SSID, LAN/DHCP, DHCP reservations, DDNS (DynDNS and No-IP, untested: needs an account), Virtual servers, Port triggering, DMZ, UPnP, ALG, DoS protection, ARP, Samba/FTP/DLNA, Date and time, LED, Reboot (+scheduled), Statistics.

Not implemented: admin-only pages (WAN, Ethernet WAN/PON, routes/RIP, QoS, VPN, firmware, log, advanced voice, USB 3G), parental control, backup/restore, factory reset.

Reversible writes (guest SSID, port forwarding, etc.) were tested against a real router. Untested: DDNS, the MLO switch, reboot, changing the router IP and security changes on the main Wi-Fi.
