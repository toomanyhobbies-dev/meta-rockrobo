SUMMARY = "Roborock R16 on-robot system image"
DESCRIPTION = "Minimal systemd image for the Roborock R16: read-only rootfs on the mainline kernel, booted by the \
stock A/B bootloader from an appended-DTB /boot/zImage. Does not yet include the robot control stack."
HOMEPAGE = "https://github.com/toomanyhobbies-dev/meta-rockrobo"
SECTION = "base"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL = "packagegroup-core-boot rockrobo-bootfiles rockrobo-base dropbear systemd-analyze"

# RTL8189ES SDIO Wi-Fi (binds mmc1 as wlan0) + userspace. cfg80211 is a kernel module (sunxi defconfig), pulled in as
# a dependency; wireless-regdb supplies the regulatory database.
IMAGE_INSTALL += "kernel-module-8189es iw wireless-regdb wpa-supplicant"

# Chassis-MCU heartbeat
IMAGE_INSTALL += "rockrobo-mcu-heartbeat"

# Full GNU userland alongside busybox (update-alternatives gives it precedence), matching the stock userland.
IMAGE_INSTALL += "coreutils util-linux iproute2 procps bash"

# Read-only rootfs; writable state lives on tmpfs and the data partition. ssh via dropbear.
IMAGE_FEATURES += "ssh-server-dropbear read-only-rootfs"

IMAGE_LINGUAS = ""

# ext4 geometry matches the stock slot (rockrobo.inc) so the 2011 U-Boot reads /boot/zImage and the stock kernel in
# the other slot can mount it.
