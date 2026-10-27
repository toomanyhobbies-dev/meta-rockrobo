# Roborock R16 robots: shared board dtsi + per-robot device trees (Kbuild builds any allwinner/*.dtb whose .dts exists,
# so copying them into the tree is enough), and the kernel config the stock bootloader needs (rockrobo.cfg).
#
# oelint note: `rockrobo` is a MACHINEOVERRIDE we define in conf/machine/include/rockrobo.inc. oelint-adv parses each
# file on its own and cannot see that, so it misreads the override-scoped lines below as global/unknown-override. The
# two `# nooelint:` lines suppress exactly those false positives (the override-scoped FILESEXTRAPATHS and ROCKROBO_DTS) and nothing else.

# nooelint: oelint.vars.noncoreoverride
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
# nooelint: oelint.vars.noncoreoverride
ROCKROBO_DTS = "sun8i-r16-roborock.dtsi sun8i-r16-roborock-s5.dts sun8i-r16-roborock-v1.dts"
# 0001-sunxi-mmc-zero-clk-phase-per-node.patch: adds the boolean DT property rockrobo,zero-clk-phase, which makes a
# controller program output/sample clock phase 0/0 above 400 kHz instead of mainline's sunxi_mmc_clk_delays table.
# The stock 3.4 BSP runs these boards' eMMC (Toshiba 004G60, 8-bit DDR52 @ 50 MHz) at delay 0/0 -- measured on the Gen 1
# and the S5 (/proc/driver/sunxi-mmc.2 hostinfo; U-Boot "sdly 25M 0" / "sdly 50M 0") -- and with the table's values the
# 8-bit bus-width test fails and DDR reads wedge. The SDIO Wi-Fi runs at the table's 50 MHz values (stock 4.8 / 6.8 ns),
# so the override must be per node, not a table edit. Set on &mmc2 in sun8i-r16-roborock.dtsi.
# 0002: reset the mmc controller after an update-clock timeout, so the S5's SDIO Wi-Fi recovers.
# rockrobo-trim.cfg: switch off the defconfig's subsystems these boards don't have or use (most of its ~617 modules).
# rockrobo-update.cfg: what RAUC needs to install a bundle.
SRC_URI:append:rockrobo = " ${@' '.join('file://' + f for f in d.getVar('ROCKROBO_DTS').split())} file://rockrobo.cfg file://rockrobo-trim.cfg file://rockrobo-update.cfg file://0001-sunxi-mmc-zero-clk-phase-per-node.patch \
                                  file://0002-sunxi-mmc-reset-the-controller-after-an-update-clock-timeout.patch"

do_configure:append:rockrobo() {
    for f in ${ROCKROBO_DTS}; do
        cp ${WORKDIR}/$f ${S}/arch/arm/boot/dts/allwinner/
    done

    # Force the kernel "command line type" to CMDLINE_EXTEND. meta-sunxi's linux.inc sets CONFIG_CMDLINE (from the
    # ${CMDLINE} bitbake var below) but the choice between FROM_BOOTLOADER / EXTEND / FORCE is only visible once
    # CONFIG_CMDLINE is non-empty, which happens AFTER the .cfg fragment is merged -- so rockrobo.cfg's
    # CONFIG_CMDLINE_EXTEND=y is silently dropped (invisible symbol) and olddefconfig then picks the default,
    # CMDLINE_FROM_BOOTLOADER, which makes the kernel ignore CONFIG_CMDLINE entirely. By now CONFIG_CMDLINE is set,
    # so re-assert the choice and re-resolve: EXTEND appends our ${CMDLINE} args to the bootloader's
    # cmdline instead of replacing it (FORCE) -- important because the stock A/B bootloader supplies root=/console=.
    sed -i -e '/CONFIG_CMDLINE_FROM_BOOTLOADER/d' -e '/CONFIG_CMDLINE_EXTEND/d' -e '/CONFIG_CMDLINE_FORCE/d' ${B}/.config
    echo 'CONFIG_CMDLINE_EXTEND=y' >> ${B}/.config
    oe_runmake -C ${S} O=${B} olddefconfig
}

# Kernel command line appended to the bootloader's (CONFIG_CMDLINE_EXTEND forced above). kernel.bbclass' do_configure
# writes CONFIG_CMDLINE from this bitbake variable, so it must be set here, not as CONFIG_CMDLINE in the .cfg fragment.
# ro: the system slot is never written.
CMDLINE:rockrobo = "ro"
