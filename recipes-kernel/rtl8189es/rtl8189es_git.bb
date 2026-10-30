SUMMARY = "Realtek RTL8189ES SDIO 802.11n Wi-Fi driver"
DESCRIPTION = "Out-of-tree driver for the Realtek RTL8189ES SDIO Wi-Fi on the Roborock R16 (sun8i-a33), which has no \
mainline driver. Builds 8189es.ko and binds the SDIO function as wlan0."
HOMEPAGE = "https://github.com/jwrdegoede/rtl8189ES_linux"
SECTION = "kernel/modules"
LICENSE = "GPL-2.0-only"
# No standalone license file in the tree; the module declares MODULE_LICENSE("GPL").
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

# Community-vetted fork (used by Arch AUR, Armbian, ophub HAOS 6.6) carrying the kernel 6.x API-compatibility
# patches. Same vendor tree as the previous EvilOlaf fork: identical Makefile, still builds 8189es.ko.
SRC_URI = "git://github.com/jwrdegoede/rtl8189ES_linux;protocol=https;branch=master"
SRCREV = "f4712611578312fcd67795a0d43d78eb70378238"
S = "${WORKDIR}/git"

inherit module

MODULE_NAME = "8189es"

# The vendor Makefile selects ARCH/CROSS_COMPILE/KSRC from a CONFIG_PLATFORM_* block; leave all unset and drive a
# plain external-module build. Command-line CC/LD/AR propagate to the kbuild sub-make via MAKEOVERRIDES.
do_compile() {
    oe_runmake modules \
        KSRC="${STAGING_KERNEL_BUILDDIR}" \
        ARCH="${ARCH}" \
        CROSS_COMPILE="${TARGET_PREFIX}" \
        CC="${KERNEL_CC}" \
        LD="${KERNEL_LD}" \
        AR="${KERNEL_AR}"
}

# The vendor install target writes to the build host's /lib/modules; install into the sysroot instead.
do_install() {
    install -d ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${S}/${MODULE_NAME}.ko ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra/
}

RPROVIDES:${PN} += "kernel-module-8189es"
KERNEL_MODULE_AUTOLOAD += "8189es"

# Power save off, as the stock firmware runs it
KERNEL_MODULE_PROBECONF += "8189es"
# Read by kernel-module-split as module_conf_<module>
# nooelint: oelint.vars.mispell.unknown
module_conf_8189es = "options 8189es rtw_power_mgnt=0 rtw_ips_mode=0 rtw_lps_level=0"
