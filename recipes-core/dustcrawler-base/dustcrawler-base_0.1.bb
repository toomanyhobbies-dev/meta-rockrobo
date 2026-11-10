SUMMARY = "Runtime configuration for dustcrawler-image"
DESCRIPTION = "systemd configuration for the dustcrawler image: volatile journald, the data-partition mount, Wi-Fi \
(wpa_supplicant + networkd DHCP), watchdog management, power-key handling, the /opt app overlay, and a lean service \
set. Root's authorized_keys is injected at build time (DUSTCRAWLER_AUTHORIZED_KEYS)."
HOMEPAGE = "https://github.com/toomanyhobbies-dev/dustcrawler-nav"
SECTION = "base"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Root's SSH authorized_keys, set by the build environment and never committed; several keys are separated by "\n".
# Empty by default: no key is baked and root SSH is unavailable until one is provided.
DUSTCRAWLER_AUTHORIZED_KEYS ??= ""

SRC_URI = "file://journald-volatile.conf \
           file://10-watchdog.conf \
           file://dropbear-rundir.conf \
           file://dropbearkey-ed25519.conf \
           file://dropbear-ed25519.conf \
           file://dustcrawler-overlay-setup.service \
           file://opt.mount \
           file://mnt-data.mount \
           file://dustcrawler-wifi.service \
           file://dustcrawler-data-guard.service \
           file://dustcrawler-data-fail.service \
           file://dustcrawler-data-guard \
           file://dustcrawler-data-fail \
           file://25-wlan0.network \
           file://logind-powerkey.conf \
           file://80-dustcrawler.preset"

S = "${UNPACKDIR}"

FILES:${PN} += "${sysconfdir}/systemd/journald.conf.d/volatile.conf \
                ${sysconfdir}/systemd/logind.conf.d/powerkey.conf \
                ${sysconfdir}/systemd/system.conf.d/10-watchdog.conf \
                ${sysconfdir}/tmpfiles.d/dropbear-rundir.conf \
                ${sysconfdir}/systemd/system/dropbearkey.service.d/ed25519.conf \
                ${sysconfdir}/systemd/system/dropbear@.service.d/ed25519.conf \
                ${sysconfdir}/systemd/network/25-wlan0.network \
                ${systemd_unitdir}/system-preset/80-dustcrawler.preset \
                ${systemd_system_unitdir}/dustcrawler-data-fail.service \
                /root/.ssh/authorized_keys \
                /mnt/data \
                /opt"

inherit systemd
SYSTEMD_SERVICE:${PN} = "mnt-data.mount dustcrawler-overlay-setup.service opt.mount dustcrawler-wifi.service \
                         dustcrawler-data-guard.service"

RDEPENDS:${PN} = "systemd wpa-supplicant"

do_install() {
    install -d ${D}${sysconfdir}/systemd/journald.conf.d
    install -m 0644 ${UNPACKDIR}/journald-volatile.conf ${D}${sysconfdir}/systemd/journald.conf.d/volatile.conf

    # Power key: ignored by logind.
    install -d ${D}${sysconfdir}/systemd/logind.conf.d
    install -m 0644 ${UNPACKDIR}/logind-powerkey.conf ${D}${sysconfdir}/systemd/logind.conf.d/powerkey.conf

    # systemd watchdog management (opens and pings /dev/watchdog).
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 ${UNPACKDIR}/10-watchdog.conf ${D}${sysconfdir}/systemd/system.conf.d/10-watchdog.conf

    # /run/dropbear for the host key (read-only rootfs keeps the key on tmpfs).
    install -d ${D}${sysconfdir}/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/dropbear-rundir.conf ${D}${sysconfdir}/tmpfiles.d/dropbear-rundir.conf

    # ed25519 host key (faster keygen than RSA); drop-ins override both dropbear units' ExecStart.
    install -d ${D}${sysconfdir}/systemd/system/dropbearkey.service.d ${D}${sysconfdir}/systemd/system/dropbear@.service.d
    install -m 0644 ${UNPACKDIR}/dropbearkey-ed25519.conf ${D}${sysconfdir}/systemd/system/dropbearkey.service.d/ed25519.conf
    install -m 0644 ${UNPACKDIR}/dropbear-ed25519.conf ${D}${sysconfdir}/systemd/system/dropbear@.service.d/ed25519.conf

    # Presets: getty@tty1 off
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/80-dustcrawler.preset ${D}${systemd_unitdir}/system-preset/80-dustcrawler.preset

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/mnt-data.mount ${D}${systemd_system_unitdir}/mnt-data.mount
    install -m 0644 ${UNPACKDIR}/dustcrawler-overlay-setup.service ${D}${systemd_system_unitdir}/dustcrawler-overlay-setup.service
    install -m 0644 ${UNPACKDIR}/opt.mount ${D}${systemd_system_unitdir}/opt.mount
    install -m 0644 ${UNPACKDIR}/dustcrawler-wifi.service ${D}${systemd_system_unitdir}/dustcrawler-wifi.service

    # No data partition: save the boot log and fall back to the other slot.
    install -m 0644 ${UNPACKDIR}/dustcrawler-data-guard.service ${D}${systemd_system_unitdir}/dustcrawler-data-guard.service
    install -m 0644 ${UNPACKDIR}/dustcrawler-data-fail.service ${D}${systemd_system_unitdir}/dustcrawler-data-fail.service
    install -d ${D}${sbindir}
    install -m 0755 ${UNPACKDIR}/dustcrawler-data-guard ${D}${sbindir}/dustcrawler-data-guard
    install -m 0755 ${UNPACKDIR}/dustcrawler-data-fail ${D}${sbindir}/dustcrawler-data-fail

    # networkd DHCP on wlan0.
    install -d ${D}${sysconfdir}/systemd/network
    install -m 0644 ${UNPACKDIR}/25-wlan0.network ${D}${sysconfdir}/systemd/network/25-wlan0.network

    # Root's authorized_keys from DUSTCRAWLER_AUTHORIZED_KEYS. Root's home is /root in this image (not OE's ROOT_HOME),
    # which is where dropbear looks. Empty -> no key baked.
    if [ -n "${DUSTCRAWLER_AUTHORIZED_KEYS}" ]; then
        install -d -m 0700 ${D}/root/.ssh
        printf '%b\n' "${DUSTCRAWLER_AUTHORIZED_KEYS}" > ${D}/root/.ssh/authorized_keys
        chmod 0600 ${D}/root/.ssh/authorized_keys
    fi

    # Mount points must exist in the image: a read-only rootfs cannot create them at runtime. /opt is the overlay
    # lowerdir (opt.mount); /mnt/data is the data-partition mount.
    install -d ${D}/mnt/data
    install -d ${D}/opt
}
