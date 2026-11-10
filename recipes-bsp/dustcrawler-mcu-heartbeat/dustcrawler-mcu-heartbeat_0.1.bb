SUMMARY = "Chassis-MCU heartbeat for the Roborock R16 robots"
DESCRIPTION = "Sends the AP heartbeat the robot's chassis MCU expects on /dev/ttyS2 once a second, and the stock \
120 s grace interval on stop. Without it the MCU presses the board's power key and, after 120 s, resets it."
HOMEPAGE = "https://github.com/toomanyhobbies-dev/dustcrawler-nav"
SECTION = "base"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://dustcrawler-mcu-heartbeat.c \
           file://dustcrawler-mcu-heartbeat.service"

S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "dustcrawler-mcu-heartbeat.service"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -Wall -Wextra -o dustcrawler-mcu-heartbeat ${S}/dustcrawler-mcu-heartbeat.c
}

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${B}/dustcrawler-mcu-heartbeat ${D}${sbindir}/dustcrawler-mcu-heartbeat
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/dustcrawler-mcu-heartbeat.service ${D}${systemd_system_unitdir}/dustcrawler-mcu-heartbeat.service
}
