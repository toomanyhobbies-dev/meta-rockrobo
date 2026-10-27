// SPDX-License-Identifier: MIT
/*
 * Keep the Roborock R16 chassis MCU's AP-comm monitor satisfied: send the 0xD3 heartbeat (interval 0) once a
 * second on the MCU link, and on SIGTERM send the 120 s grace interval stock sends when its chassis process exits,
 * so the MCU does not reset or power-key the board while it reboots. Frames: docs/mcu-protocol.md.
 */
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <termios.h>
#include <time.h>
#include <unistd.h>

static const unsigned char heartbeat[] = { 0xAA, 0x06, 0x01, 0xD3, 0x04, 0x00, 0x00, 0x00, 0x00, 0x82 };
static const unsigned char grace[] = { 0xAA, 0x06, 0x01, 0xD3, 0x04, 0x78, 0x00, 0x00, 0x00, 0x36 };

static volatile sig_atomic_t stop;

static void on_signal(int sig)
{
	(void)sig;
	stop = 1;
}

static int open_link(const char *dev)
{
	struct termios tio;
	int fd = open(dev, O_RDWR | O_NOCTTY | O_CLOEXEC);

	if (fd < 0)
		return -1;
	memset(&tio, 0, sizeof(tio));
	cfmakeraw(&tio);
	tio.c_cflag |= CLOCAL | CREAD;
	cfsetispeed(&tio, B1152000);
	cfsetospeed(&tio, B1152000);
	if (tcsetattr(fd, TCSANOW, &tio) < 0) {
		close(fd);
		return -1;
	}
	return fd;
}

static int send_frame(int fd, const unsigned char *f, size_t len)
{
	ssize_t n = write(fd, f, len);

	return n == (ssize_t)len ? 0 : -1;
}

int main(int argc, char **argv)
{
	const char *dev = argc > 1 ? argv[1] : "/dev/ttyS2";
	struct sigaction sa = { .sa_handler = on_signal };
	struct timespec next;
	int fd;

	sigaction(SIGTERM, &sa, NULL);
	sigaction(SIGINT, &sa, NULL);

	fd = open_link(dev);
	if (fd < 0) {
		fprintf(stderr, "rockrobo-mcu-heartbeat: %s: %s\n", dev, strerror(errno));
		return 1;
	}
	fprintf(stderr, "rockrobo-mcu-heartbeat: heartbeat on %s every 1 s\n", dev);

	clock_gettime(CLOCK_MONOTONIC, &next);
	while (!stop) {
		if (send_frame(fd, heartbeat, sizeof(heartbeat)) < 0) {
			fprintf(stderr, "rockrobo-mcu-heartbeat: write: %s\n", strerror(errno));
			return 1;
		}
		/* drop whatever the MCU streams at us; nobody else reads this link */
		tcflush(fd, TCIFLUSH);
		next.tv_sec += 1;
		while (!stop && clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &next, NULL) == EINTR)
			;
	}

	if (send_frame(fd, grace, sizeof(grace)) < 0) {
		fprintf(stderr, "rockrobo-mcu-heartbeat: grace frame: %s\n", strerror(errno));
		return 1;
	}
	tcdrain(fd);
	fprintf(stderr, "rockrobo-mcu-heartbeat: sent the 120 s grace interval\n");
	return 0;
}
