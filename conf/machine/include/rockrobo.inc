# Roborock "Ruby-R16" boards (S5, Gen 1): Allwinner R16, 512 MB, eMMC with the stock A/B layout
# (system_a = mmcblk0p8, system_b = mmcblk0p9, 512 MiB each, ext4, kernel at /boot/zImage).
# The stock U-Boot stays: it implements the A/B fallback (docs/boot-ab-findings.md), so no bootloader is built.
require conf/machine/include/sun8i.inc
MACHINEOVERRIDES =. "rockrobo:"

KERNEL_IMAGETYPE = "zImage"
KERNEL_DEVICETREE = "allwinner/${ROCKROBO_DTB}"
SERIAL_CONSOLES = "115200;ttyS0"

MACHINE_FEATURES = "serial"

# One system slot = one ext4 filesystem, made the way the stock one is: the 2011 U-Boot has to read /boot/zImage from
# it and the stock 3.4 kernel in the other slot has to be able to mount it. Stock: 1 KiB blocks, 128-byte inodes,
# no 64bit / metadata_csum (e2fsprogs 1.47 would enable those and orphan_file by default), 522240 blocks.
IMAGE_FSTYPES = "ext4 ext4.gz"
EXTRA_IMAGECMD:ext4 = "-b 1024 -I 128 -i 4096 -O ^64bit,^metadata_csum,^metadata_csum_seed,^orphan_file,uninit_bg"
IMAGE_ROOTFS_SIZE = "522240"
IMAGE_ROOTFS_MAXSIZE = "522240"
IMAGE_OVERHEAD_FACTOR = "1.0"
IMAGE_ROOTFS_EXTRA_SPACE = "0"

# The stock U-Boot loads only a zImage (no separate initrd). The poky probe bundles its initramfs into the zImage;
# the rockrobo distro boots its own rootfs from a plain zImage. Gate on DISTRO (set after local.conf parsing, so not
# overridable from a kas file).
INITRAMFS_IMAGE = "rockrobo-probe-initramfs"
INITRAMFS_IMAGE_BUNDLE = "${@'1' if d.getVar('DISTRO') == 'poky' else '0'}"
