# Root is the only account and login is key-only. Stock OE dropbear ships DROPBEAR_EXTRA_ARGS="-w" (no root login at
# all, key auth included); use "-s" instead (no password auth, root permitted by key). Redirect the host key to tmpfs
# because the rootfs is read-only (rockrobo-base provides the matching /run/dropbear tmpfiles entry). Scoped to
# the rockrobo MACHINEOVERRIDE.
do_install:append:rockrobo() {
    if [ -f ${D}${sysconfdir}/default/dropbear ]; then
        sed -i 's/^DROPBEAR_EXTRA_ARGS=.*/DROPBEAR_EXTRA_ARGS="-s"/' ${D}${sysconfdir}/default/dropbear
        echo 'DROPBEAR_RSAKEY_DIR=/run/dropbear' >> ${D}${sysconfdir}/default/dropbear
    fi
}
