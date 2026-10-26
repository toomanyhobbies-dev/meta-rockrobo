SUMMARY = "Runtime configuration for rockrobo-image"
DESCRIPTION = "systemd configuration for the rockrobo image: volatile journald, the data-partition mount, Wi-Fi \
(wpa_supplicant + networkd DHCP), watchdog management, power-key handling, the /opt app overlay, and a lean service \
set. Root's authorized_keys is injected at build time (ROCKROBO_AUTHORIZED_KEYS)."
HOMEPAGE = "https://github.com/toomanyhobbies-dev/meta-rockrobo"
SECTION = "base"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Root's SSH authorized_keys, set by the build environment and never committed; several keys are separated by "\n".
# Empty by default: no key is baked and root SSH is unavailable until one is provided.
ROCKROBO_AUTHORIZED_KEYS ??= ""

SRC_URI = "file://journald-volatile.conf \
           file://10-watchdog.conf \
           file://dropbear-rundir.conf \
           file://dropbearkey-ed25519.conf \
           file://dropbear-ed25519.conf \
           file://rockrobo-overlay-setup.service \
           file://opt.mount \
           file://mnt-data.mount \
           file://rockrobo-wifi.service \
           file://25-wlan0.network \
           file://logind-powerkey.conf"

# Masked (symlinked to /dev/null): busybox syslog/klogd duplicate journald; getty@tty1 has no VT on this headless
# board (console is serial).
MASKED_UNITS = "busybox-syslog.service busybox-klogd.service getty@tty1.service"

FILES:${PN} += "${sysconfdir}/systemd/journald.conf.d/volatile.conf \
                ${sysconfdir}/systemd/logind.conf.d/powerkey.conf \
                ${sysconfdir}/systemd/system.conf.d/10-watchdog.conf \
                ${sysconfdir}/tmpfiles.d/dropbear-rundir.conf \
                ${sysconfdir}/systemd/system/dropbearkey.service.d/ed25519.conf \
                ${sysconfdir}/systemd/system/dropbear@.service.d/ed25519.conf \
                ${sysconfdir}/systemd/network/25-wlan0.network \
                ${sysconfdir}/systemd/system/busybox-syslog.service \
                ${sysconfdir}/systemd/system/busybox-klogd.service \
                ${sysconfdir}/systemd/system/getty@tty1.service \
                /root/.ssh/authorized_keys \
                /mnt/data \
                /opt"

inherit systemd
SYSTEMD_SERVICE:${PN} = "mnt-data.mount rockrobo-overlay-setup.service opt.mount rockrobo-wifi.service"

RDEPENDS:${PN} = "systemd wpa-supplicant"

do_install() {
    install -d ${D}${sysconfdir}/systemd/journald.conf.d
    install -m 0644 ${WORKDIR}/journald-volatile.conf ${D}${sysconfdir}/systemd/journald.conf.d/volatile.conf

    # Power key: ignored by logind.
    install -d ${D}${sysconfdir}/systemd/logind.conf.d
    install -m 0644 ${WORKDIR}/logind-powerkey.conf ${D}${sysconfdir}/systemd/logind.conf.d/powerkey.conf

    # systemd watchdog management (opens and pings /dev/watchdog).
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 ${WORKDIR}/10-watchdog.conf ${D}${sysconfdir}/systemd/system.conf.d/10-watchdog.conf

    # /run/dropbear for the host key (read-only rootfs keeps the key on tmpfs).
    install -d ${D}${sysconfdir}/tmpfiles.d
    install -m 0644 ${WORKDIR}/dropbear-rundir.conf ${D}${sysconfdir}/tmpfiles.d/dropbear-rundir.conf

    # ed25519 host key (faster keygen than RSA); drop-ins override both dropbear units' ExecStart.
    install -d ${D}${sysconfdir}/systemd/system/dropbearkey.service.d ${D}${sysconfdir}/systemd/system/dropbear@.service.d
    install -m 0644 ${WORKDIR}/dropbearkey-ed25519.conf ${D}${sysconfdir}/systemd/system/dropbearkey.service.d/ed25519.conf
    install -m 0644 ${WORKDIR}/dropbear-ed25519.conf ${D}${sysconfdir}/systemd/system/dropbear@.service.d/ed25519.conf

    # Mask units (symlink to /dev/null, as `systemctl mask`).
    for u in ${MASKED_UNITS}; do
        ln -sf /dev/null ${D}${sysconfdir}/systemd/system/$u
    done

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/mnt-data.mount ${D}${systemd_system_unitdir}/mnt-data.mount
    install -m 0644 ${WORKDIR}/rockrobo-overlay-setup.service ${D}${systemd_system_unitdir}/rockrobo-overlay-setup.service
    install -m 0644 ${WORKDIR}/opt.mount ${D}${systemd_system_unitdir}/opt.mount
    install -m 0644 ${WORKDIR}/rockrobo-wifi.service ${D}${systemd_system_unitdir}/rockrobo-wifi.service

    # networkd DHCP on wlan0.
    install -d ${D}${sysconfdir}/systemd/network
    install -m 0644 ${WORKDIR}/25-wlan0.network ${D}${sysconfdir}/systemd/network/25-wlan0.network

    # Root's authorized_keys from ROCKROBO_AUTHORIZED_KEYS. Root's home is /root in this image (not OE's ROOT_HOME),
    # which is where dropbear looks. Empty -> no key baked.
    if [ -n "${ROCKROBO_AUTHORIZED_KEYS}" ]; then
        install -d -m 0700 ${D}/root/.ssh
        printf '%b\n' "${ROCKROBO_AUTHORIZED_KEYS}" > ${D}/root/.ssh/authorized_keys
        chmod 0600 ${D}/root/.ssh/authorized_keys
    fi

    # Mount points must exist in the image: a read-only rootfs cannot create them at runtime. /opt is the overlay
    # lowerdir (opt.mount); /mnt/data is the data-partition mount.
    install -d ${D}/mnt/data
    install -d ${D}/opt
}
