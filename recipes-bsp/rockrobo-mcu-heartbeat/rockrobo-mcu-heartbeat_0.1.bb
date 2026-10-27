SUMMARY = "Chassis-MCU heartbeat for the Roborock R16 robots"
DESCRIPTION = "Sends the AP heartbeat the robot's chassis MCU expects on /dev/ttyS2 once a second, and the stock \
120 s grace interval on stop. Without it the MCU presses the board's power key and, after 120 s, resets it."
HOMEPAGE = "https://github.com/toomanyhobbies-dev/meta-rockrobo"
SECTION = "base"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://rockrobo-mcu-heartbeat.c \
           file://rockrobo-mcu-heartbeat.service"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "rockrobo-mcu-heartbeat.service"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -Wall -Wextra -o rockrobo-mcu-heartbeat ${S}/rockrobo-mcu-heartbeat.c
}

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${B}/rockrobo-mcu-heartbeat ${D}${sbindir}/rockrobo-mcu-heartbeat
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/rockrobo-mcu-heartbeat.service ${D}${systemd_system_unitdir}/rockrobo-mcu-heartbeat.service
}
