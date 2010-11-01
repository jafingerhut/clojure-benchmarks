#include <windows.h>
#include <stdio.h>

int main (int argc, char *argv[])
{
    LPCH x = GetEnvironmentStrings();
    while (*x != '\0') {
        printf("%s\n", x);
        x += strlen(x);
        x++;
    }
    return 0;
}
