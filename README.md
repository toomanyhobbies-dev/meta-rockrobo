# meta-rockrobo

Yocto/OpenEmbedded BSP and distro layer for the Roborock R16 robots (Allwinner sun8i-a33): a mainline 6.6 kernel
booted by the stock A/B bootloader, the RTL8189ES SDIO Wi-Fi driver, and a lean read-only systemd image.

## Contents
- `conf/` — the `rockrobo` machine definitions (`rockrobo-v1`, `rockrobo-s5`) and the `rockrobo` distro.
- `recipes-kernel/` — mainline kernel config, device trees, the eMMC clock-phase patch, and the RTL8189ES Wi-Fi driver.
- `recipes-core/` — `rockrobo-base` (runtime systemd config) and `rockrobo-image` (the read-only systemd image).

## Usage
Add this layer alongside `openembedded-core` and `meta-sunxi` (scarthgap release), set `MACHINE` to `rockrobo-v1`
or `rockrobo-s5`, and build `rockrobo-image`.

## License
MIT (see `LICENSE`). Components this layer fetches or patches carry their own terms — notably the kernel phase patch
and the RTL8189ES driver are GPL-2.0; see each recipe's `LICENSE` field.
