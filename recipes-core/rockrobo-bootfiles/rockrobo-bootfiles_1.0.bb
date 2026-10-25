SUMMARY = "/boot/zImage with the device tree appended, for the stock Roborock U-Boot"
DESCRIPTION = "The stock 2011.09 U-Boot runs `ext4load mmc 2:<slot> /boot/zImage; bootz` with no FDT \
argument, so the device tree must be concatenated onto the zImage (CONFIG_ARM_APPENDED_DTB). This recipe \
produces that single /boot/zImage file as a package the image installs, so BitBake's signatures track the \
kernel: do_install[depends] on the kernel's do_deploy means a kernel/config/DTS change re-runs this \
do_install, and IMAGE_INSTALL carries that into do_rootfs. (Doing the concatenation in a \
ROOTFS_POSTPROCESS_COMMAND reading DEPLOY_DIR instead leaves it outside the signature graph -> stale images.)"
HOMEPAGE = "https://github.com/toomanyhobbies-dev/meta-rockrobo"
SECTION = "kernel"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "virtual/kernel"
# no sources of its own: the inputs are the kernel's deployed zImage and DTB (see do_install[depends])
SRC_URI = ""

COMPATIBLE_MACHINE = "rockrobo"
# the concatenated image is machine-specific (per-robot DTB), so the package is too
PACKAGE_ARCH = "${MACHINE_ARCH}"

# order AND signature: the kernel's do_deploy sstate hash feeds this task's signature, so a kernel change
# (config/patch/DTS) re-runs the concatenation below with no nostamp needed.
do_install[depends] += "virtual/kernel:do_deploy"

do_install() {
    install -d ${D}/boot
    # Use the initramfs-bundled kernel when INITRAMFS_IMAGE_BUNDLE is set, else the plain zImage. Key off the
    # flag and the deterministic ${MACHINE} symlink, never an ls -t glob (a stale zImage-initramfs*.bin from an
    # earlier bundled build would otherwise be picked).
    if [ "${INITRAMFS_IMAGE_BUNDLE}" = "1" ]; then
        k="${DEPLOY_DIR_IMAGE}/zImage-initramfs-${MACHINE}.bin"
    else
        k="${DEPLOY_DIR_IMAGE}/zImage-${MACHINE}.bin"
    fi
    cat "$k" "${DEPLOY_DIR_IMAGE}/${ROCKROBO_DTB}" > ${D}/boot/zImage
}

FILES:${PN} += "/boot/zImage"
