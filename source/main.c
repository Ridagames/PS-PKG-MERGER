#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/process.h>
#include <sysutil/sysutil.h>

SYS_PROCESS_PARAM(1001, 0x20000)

#define LV2_SYSCALL_LV2_PEEK 8

static inline uint64_t lv2_peek(uint64_t addr)
{
    register uint64_t r3  __asm__("3")  = addr;
    register uint64_t r11 __asm__("11") = LV2_SYSCALL_LV2_PEEK;
    __asm__ volatile("sc"
        : "+r"(r3)
        : "r"(r11)
        : "4","5","6","7","8","9","10","12","memory");
    return r3;
}

int main(int argc, char **argv)
{
    (void)argc; (void)argv;
    printf("PS3 Kernel Reader - Actif !\n");

    uint64_t val = lv2_peek(0x8000000000000000ULL);
    printf("Peek 0x8000... : 0x%016llX\n", (unsigned long long)val);
    fflush(stdout);

    for (;;) {
        sysUtilCheckCallback();
        usleep(16000);
    }
    return 0;
}
