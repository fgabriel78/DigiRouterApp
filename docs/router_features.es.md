# Inventario de funciones – DIGI Wi-Fi 7 (TP-Link XGB430v Pro, FW 0.2.0 3.2.1 Build 250808)

🇬🇧 [English version](router_features.md)

Generado automáticamente a partir del menú real del router (`frame/menu.htm`) evaluado con las banderas de funciones del dispositivo y las páginas realmente instaladas (`menu.cgi`).

- **U** = visible con la cuenta `user` (rol *User*). **A** = solo con `admin` (rol *Admin*).
- "Objetos" son los objetos del modelo de datos (`DEV2_*`, `ACT_*`) que lee/escribe la página en el router.

## Menú avanzado


### Estado

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `status.htm` | Estado (WAN/PON, IPv4/IPv6) | `DEV2_GPON_INTF_OMCI_STATS` `DEV2_GPON_INTF_STATS` `DEV2_OPTC_GPON_CFG` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` |

### Internet / Red

| Rol | Página | Función | Objetos |
|---|---|---|---|
| A | `ethWan.htm` | Ethernet WAN | `DEV2_ADT_WAN` `DEV2_ETH` `DEV2_ETH_INTF` `DEV2_FIREWALL` `DEV2_FLOWCONTROL` `DEV2_INTERNET_DOMAIN` `DEV2_IQOS` `DEV2_MANAGEMENT_SERVER` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_X_TP_DEFAULTGATEWAY` |
| A | `wan.htm` | Red / WAN | `DEV2_ADT_WAN` `DEV2_DSL_CHANNEL` `DEV2_OPTC_INTF` |
| U | `dhcp.htm` | LAN / servidor DHCP / reservas | `DEV2_ADT_LAN` `DEV2_ARP_BIND_ENTRY` `DEV2_DHCPV4_POOL_STATICADDR` `DEV2_DHCPV4_SERVER_POOL` `DEV2_FLOWCONTROL` `DEV2_HOST_ENTRY` |
| A | `gponcfg.htm` | Configuración PON (GPON/XGS-PON) | — |
| U | `ddns.htm` | DNS dinámico | `CLOUD_DDNS` `CLOUD_DDNS_ENTRY` `DEV2_CURRENT_USER` `DEV2_DYN_DNS_CFG` `DEV2_NOIP_DNS_CFG` `DEV2_USERDEFINE_DDNS_CFG` |
| A | `route.htm` | Enrutamiento avanzado | `DEV2_ADT_WAN` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` |
| A | `rip.htm` | RIP | `DEV2_ADT_WAN` `DEV2_RIP` |

### Wi-Fi

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `wirelessSettings.htm` | Ajustes Wi-Fi por banda (canal, ancho, potencia, estándar) | `ACT_AUTO_CHAN_SELECT` `ACT_WIFI_RELOAD_MLO` `DEV2_ADT_WIFI_COMMON` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` `DEV2_WIFI_RADIO` `DEV2_XTP_GPIO_BTN` |
| U | `wps.htm` | WPS | `ACT_WIFI_GET_NEW_PIN` `ACT_WIFI_RESTORE_PIN` `DEV2_ADT_WIFI_COMMON` |
| U | `macFilter.htm` | Filtro MAC Wi-Fi | `DEV2_ADT_WIFI_CLIENT` `DEV2_ADT_WIFI_COMMON` `DEV2_ADT_WIFI_MACTABLE` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV_AFFSTAINFO` |
| U | `wirelessSchedule.htm` | Programación Wi-Fi | `ACT_WIFI_RELOAD_MLO` `DEV2_ADT_WIFI_SCHEDULE` |
| U | `wirelessStat.htm` | Estadísticas Wi-Fi / clientes | `ACT_WIFI_UPDATE_ALLASSOC` `DEV2_ADT_WIFI_CLIENT` `DEV2_ADT_WIFI_COMMON` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV_ASSOCDEV` |
| U | `wirelessAdv.htm` | Wi-Fi avanzado | `ACT_WIFI_UPDATE_ASSOC` `DEV2_ADT_WIFI_COMMON` `DEV2_DHCPV4_SERVER_POOL` `DEV2_X_TP_EASYMESH` |
| U | `multiSSID.htm` | Multi-SSID | `DEV2_ADT_WIFI_COMMON` `DEV2_HOUR` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` `DEV2_WIFI_SCHEDULE` |
| U | `easyMesh.htm` | EasyMesh | `ACT_FACTORY_RESET` `ACT_REBOOT` `DEV2_ADT_WIFI_COMMON` `DEV2_DEV_INFO` `DEV2_LOCAL` `DEV2_WIFI_APDEV` `DEV2_X_TP_AGENTINFO` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_ONBOARDBYSCANNING` |

### Red de invitados

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `wlGuestDulBandAdv.htm` | Red de invitados (avanzado) | `DEV2_ADT_WIFI_COMMON` `DEV2_IQOS` `DEV2_WIFI_BANDSTEERING` `DEV2_WIFI_PASSWORD_RULE` |

### Telefonía

| Rol | Página | Función | Objetos |
|---|---|---|---|
| A | `voice_advance.htm` | Telefonía avanzada (admin) | `DEV2_ADT_WAN` `DEV2_LOCAL` `XTP_MULTIISP_CODEC_LIST` `XTP_VOICE_CALL_FWD_LIST` `XTP_VOICESERVICE` |
| A | `voice_telebook.htm` | Agenda | — |
| U | `voice_calllog.htm` | Registro de llamadas | — |
| A | `voice_digitmap.htm` | Plan de marcación | — |
| A | `voice_callblocks.htm` | Bloqueo de llamadas | `DEV2_LOCAL` |
| A | `voice_callforward.htm` | Desvío de llamadas | — |

### Reenvío de puertos / NAT

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `alg.htm` | ALG | `DEV2_ALG_CFG` |
| U | `virtualServer.htm` | Servidores virtuales (reenvío de puertos) | `DEV2_ADT_WAN` `DEV2_IP_INTF` `DEV2_PORTMAPPING` `DEV2_ROUTER_V4FWD` |
| U | `portTrigger.htm` | Port triggering | `DEV2_ADT_WAN` `DEV2_PORTTRIGGERING` `DEV2_ROUTER_V4FWD` |
| U | `dmz.htm` | DMZ | `DEV2_ADT_LAN` `DEV2_DMZ_HOST_CFG` |
| U | `upnp.htm` | UPnP | `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` |

### USB

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `diskSettings.htm` | Ajustes de disco | `DEV2_LOGICAL_VOLUME` `DEV2_USB_DEVICE` |
| U | `folderSharing.htm` | Carpetas compartidas | `DEV2_ADT_LAN` `DEV2_ADT_WAN` `DEV2_DLNA_FOLDER` `DEV2_DLNA_MEDIA_SERVER` `DEV2_DMZ_HOST_CFG` `DEV2_FOLDER_BROWSE` `DEV2_FOLDER_NODE` `DEV2_FTP_SERVER` `DEV2_FTP_SERVER_FOLDER` `DEV2_LOGICAL_VOLUME` `DEV2_PORTMAPPING` `DEV2_PORTTRIGGERING` `DEV2_ROUTER_V4FWD` `DEV2_SMB_SERVICE` `DEV2_SMB_SERVICE_FOLDER` `DEV2_SMB_USER_ACCESS` `DEV2_STORAGE_HTTP` `DEV2_STORAGE_HTTP_FOLDER` `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` `DEV2_USB_DEVICE` `DEV2_USER_ACCOUNT` `DEV2_USER_CFG` |
| A | `usb3g.htm` | USB 3G | `DEV2_ADT_WAN` `DEV2_SYSMODE` `DEV2_USB` |

### QoS

| Rol | Página | Función | Objetos |
|---|---|---|---|
| A | `iqosSettings.htm` | QoS | `DEV2_GAME_QOS_CLIENT` `DEV2_HOST_ENTRY` `DEV2_IQOS` `DEV2_IQOS_RULE` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_WIFI_APDEV` |

### Seguridad

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `portFiltering.htm` | Filtrado de puertos / firewall | `DEV2_ADT_LAN` `DEV2_FIREWALL` `DEV2_FW_CHAIN_RULE` |
| U | `ddos.htm` | Protección DoS | `DEV2_DDOS_CFG` `DEV2_DOS_HOST` `DEV2_FIREWALL` `DEV2_STAT_CFG` |
| U | `accessControl.htm` | Control de acceso | `/cgi/info` `DEV2_FIREWALL` `DEV2_FW_CHAIN` `DEV2_FW_CHAIN_RULE` `DEV2_HOST_ENTRY` `DEV2_WIFI_ACCESSCONTROL` `DEV2_WIFI_APDEV` `DEV2_WIFI_APDEV_ASSOCDEV` `DEV2_WIFI_APDEV_ETHASSOCDEV` `DEV2_X_TP_ONEMESH_DEVICE` |
| U | `arpBind.htm` | Enlace ARP | `DEV2_ARP_BIND` `DEV2_ARP_BIND_ENTRY` `DEV2_ARP_ENTRY` `DEV2_DHCPV4_POOL_STATICADDR` `DEV2_HOST_ENTRY` `DEV2_WIFI_APDEV` `DEV2_X_TP_ONEMESH_DEVICE` |
| U | `ipv6Firewall.htm` | Firewall IPv6 | `DEV2_ADT_WAN` `DEV2_PORTMAPPING` `DEV2_ROUTER_V6FWD` |

### VPN

| Rol | Página | Función | Objetos |
|---|---|---|---|
| A | `openvpnServer.htm` | Servidor OpenVPN | `DEV2_OPENVPN` |
| A | `pptpvpnServer.htm` | Servidor PPTP | — |
| A | `ipsec.htm` | IPSec | `DEV2_IPSEC` `DEV2_IPSEC_CFG` |
| A | `vpnStatus.htm` | Estado VPN | — |

### Herramientas / Sistema

| Rol | Página | Función | Objetos |
|---|---|---|---|
| U | `time.htm` | Fecha y hora | `DEV2_ADT_WAN` `DEV2_HOUR` `DEV2_LOCAL` `DEV2_TIME` |
| U | `ledSchedule.htm` | Programación de LED | `DEV2_LED_SCHEDULE_CFG` |
| U | `diagnostic.htm` | Diagnóstico (ping, traceroute, nslookup) | `ACT_DIAG_WEB_INTERNETDIAG` `DEV2_ADT_WAN` `DEV2_DIAG_IPPING` `DEV2_DIAG_NSLOOKUP` `DEV2_DIAG_TOOL` `DEV2_DIAG_TRACERT` `DEV2_DSL_LINE` `DEV2_ETH_INTF` `DEV2_FAST_LINE` `DEV2_IP_INTF` `DEV2_NSLOOKUP_RESULT` `DEV2_OPTC_GPON_CFG` `DEV2_ROUTE_HOPS` `DEV2_ROUTE_HOPSV6` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_USB_INTF` |
| A | `softup.htm` | Actualización de firmware | `/cgi/localMeshUpgrade` `ACT_DIAG_DNSDIAG` `CLOUD_SERVICE` `DEV2_CELL_INTF` `DEV2_DEV_INFO` `DEV2_DIAG_TOOL` `DEV2_HOUR` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_SYS_CFG` `DEV2_WIFI_APDEV` `DEV2_WIFI_APDEV_COMPONENT` `DEV2_WIFI_RADIO` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_UPGRADE` `DEV2_XTP_LTE` `FW_AUTO_UPGRADE` `FW_UPGRADE_INFO` `RE_FW_UPGRADE_INFO` |
| U | `backNRestore.htm` | Copia de seguridad / restaurar | `ACT_REBOOT` `DEV2_CURRENT_USER` `DEV2_DEV_INFO` `DEV2_SYS_CFG` |
| U | `restartSchedule.htm` | Reinicio (y programación) | `DEV2_HOUR` `DEV2_REBOOT_SCHEDULE_CFG` `DEV2_TIME` |
| U | `manageCtrl.htm` | Administración (cuentas, acceso remoto, SSH/Telnet, restaurar fábrica) | `/cgi/auth` `/cgi/info` `/cgi/logout` `ACT_FACTORY_RESET` `ACT_OPTION66_RESET` `ACT_REBOOT` `CLOUD_DDNS` `DEV2_ACL_CFG` `DEV2_ADT_LAN` `DEV2_ADT_WAN` `DEV2_CURRENT_USER` `DEV2_DHCPV4_SERVER_POOL` `DEV2_DMZ_HOST_CFG` `DEV2_DYN_DNS_CFG` `DEV2_HTTP_CFG` `DEV2_NOIP_DNS_CFG` `DEV2_PAGE_ACCESS_PERMISSION` `DEV2_PAGE_OPERATIONMODE` `DEV2_PORTMAPPING` `DEV2_PORTTRIGGERING` `DEV2_PPP` `DEV2_ROUTER_V4FWD` `DEV2_ROUTER_V6FWD` `DEV2_SSH_CFG` `DEV2_SUPER_HTTP_CFG` `DEV2_TELNET_CFG` `DEV2_UPNP_CFG` `DEV2_UPNP_PORTMAPPING` `DEV2_USER_CFG` `DEV2_USERDEFINE_DDNS_CFG` `DEV2_USERS_USER` `DEV2_X_TP_EASYMESH` `DEV2_X_TP_ISPRESTORE` |
| A | `log.htm` | Registro del sistema | `DEV2_PAGE_ACCESS_PERMISSION` |
| U | `stat.htm` | Estadísticas de tráfico | `DEV2_DDOS_CFG` `DEV2_STAT_CFG` `DEV2_STAT_ENTRY` |
| U | `sessionTimeout.htm` | Tiempo de sesión | `SESSION_TIMEOUT` |

## Vistas básicas adicionales

| Rol | Página | Función |
|---|---|---|
| U | `networkMap.htm` | Mapa de red / dispositivos conectados |
| A | `wanBasic.htm` | Internet (básico) |
| U | `wirelessBasic.htm` | Wi-Fi básico (SSID, contraseña, bandas, MLO) |
| U | `wlGuestDulBandBasic.htm` | Red de invitados (básico) |
| U | `multiSSIDBasic.htm` | Multi-SSID básico |
| A | `voice_basic.htm` | Telefonía básica (admin) |
| U | `voice_telephony.htm` | Dispositivos telefónicos / DECT |
| U | `usbManage.htm` | USB / almacenamiento (SMB, FTP, DLNA) |
| A | `basic3g.htm` | 3G básico |
| U | `parentCtrl_v2.htm` | Control parental |

## Resumen

- Páginas del menú avanzado visibles con `user`: **33**
- Páginas adicionales solo con `admin`: **18**

> Las páginas de "Admin" (WAN/PON, enrutamiento, QoS, VPN, firmware, registro, telefonía avanzada...) solo se ven en la web con la cuenta `admin`. La app las oculta con `user` y no están implementadas.

## Estado de implementación en la app

Implementadas (rol user, 23 pantallas): Resumen, Dispositivos, Wi-Fi principal + MLO, Radio, Wi-Fi avanzado + band steering, WPS, Invitados, Multi-SSID, LAN/DHCP, Reservas DHCP, DDNS (DynDNS y No-IP, sin probar: requiere cuenta), Servidores virtuales, Port triggering, DMZ, UPnP, ALG, Protección DoS, ARP, Samba/FTP/DLNA, Fecha y hora, LED, Reinicio (+programado), Estadísticas.

No implementadas: páginas solo admin (WAN, Ethernet WAN/PON, rutas/RIP, QoS, VPN, firmware, log, voz avanzada, USB 3G), control parental, copia/restauración, restablecimiento de fábrica.

Las escrituras reversibles (SSID de invitados, reenvío de puertos, etc.) se probaron contra el router real. Sin probar: DDNS, interruptor MLO, reinicio, cambio de IP del router y cambios de seguridad en el Wi-Fi principal.
