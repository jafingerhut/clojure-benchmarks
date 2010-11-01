#include <windows.h>
#include <stdio.h>
#include <tchar.h>
#include <psapi.h>

typedef unsigned long long uint64;


//////////////////////////////////////////////////////////////////////

// Parts of this program were originally copied from this web page:

// http://msdn.microsoft.com/en-us/library/ms682050%28VS.85%29.aspx

// Significant modifications have been made since then.

// I believe this web page may contain documentation on the fields of
// the PROCESS_MEMORY_COUNTERS struct:

// http://msdn.microsoft.com/en-us/library/ms684874%28v=VS.85%29.aspx

// Actually, that web page seems to document the fields of a
// PROCESS_MEMORY_COUNTERS_EX struct, which is the same as
// PROCESS_MEMORY_COUNTERS, except that the "_EX" version contains an
// extra field "DWORD PrivateUsage" at the end.  Also in
// /usr/include/w32api/psapi.h where PROCESS_MEMORY_COUNTERS is
// defined on Cygwin, it uses DWORD for the type of all fields,
// whereas on Microsoft's web page it is only DWORD for the first two
// field cb and PageFaultcount, but SIZE_T for the rest.  Hopefully
// those are the same size.

//////////////////////////////////////////////////////////////////////

// Documentation about the Windows working set of memory pages:
// http://msdn.microsoft.com/en-us/library/cc441804%28v=VS.85%29.aspx

// More documentation about the various types of pages that are
// counted in the PROCESS_MEMORY_COUNTERS structure:
// http://msdn.microsoft.com/en-us/library/ms684879%28v=VS.85%29.aspx
// http://msdn.microsoft.com/en-us/library/ms684874%28VS.85%29.aspx


void PrintMemoryAndTimeInfo (DWORD processID)
{
    HANDLE hProcess;
    PROCESS_MEMORY_COUNTERS pmc;
    FILETIME CreationTime;
    FILETIME ExitTime;
    FILETIME KernelTime;
    FILETIME UserTime;

    // Print the process identifier.
    fprintf(stderr, "\nProcess ID: %u\n", processID);

    // Get a handle for the process
    hProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                           FALSE, processID);
    if (NULL == hProcess) {
        fprintf(stderr, " OpenProcess() returned NULL\n");
        return;
    }

    // Print information about the cpu time of the process.
    // Documentation for GetProcessTimes() is available here:
    // http://msdn.microsoft.com/en-us/library/ms683223%28VS.85%29.aspx
    if (GetProcessTimes(hProcess, &CreationTime, &ExitTime,
                        &KernelTime, &UserTime)) {
        uint64 ctime = (((uint64) CreationTime.dwHighDateTime << 32)
                        + (uint64) CreationTime.dwLowDateTime);
        uint64 etime = (((uint64) ExitTime.dwHighDateTime << 32)
                        + (uint64) ExitTime.dwLowDateTime);
        uint64 ktime = (((uint64) KernelTime.dwHighDateTime << 32)
                        + (uint64) KernelTime.dwLowDateTime);
        uint64 utime = (((uint64) UserTime.dwHighDateTime << 32)
                        + (uint64) UserTime.dwLowDateTime);

        // ktime and utime are given to us in units of 100s of
        // nanoseconds.
        fprintf(stderr, "    elapsed time (seconds): %.2f\n",
                (etime - ctime) / 10000000.0);
        fprintf(stderr, "    user time (seconds): %.2f\n",
                utime / 10000000.0);
        fprintf(stderr, "    kernel time (seconds): %.2f\n",
                ktime / 10000000.0);
    } else {
        fprintf(stderr, "    GetProcessTimes() returned NULL\n");
    }

    // Print information about the memory usage of the process.
    if (GetProcessMemoryInfo(hProcess, &pmc, sizeof(pmc))) {
        fprintf(stderr, "    Page Fault Count: %u\n",
                pmc.PageFaultCount);
        fprintf(stderr, "    Peak Working Set Size (kbytes): %u\n",
                (pmc.PeakWorkingSetSize + 1023) / 1024);
        fprintf(stderr, "    Quota Peak Paged Pool Usage: %u\n",
                pmc.QuotaPeakPagedPoolUsage);
        fprintf(stderr, "    Quota Peak Non Paged Pool Usage: %u\n",
                pmc.QuotaPeakNonPagedPoolUsage);
        fprintf(stderr, "    Peak Pagefile Usage: %u\n",
                pmc.PeakPagefileUsage);

        // Don't bother to print these statistics, since they are most
        // likely garbage anyway, by the time the process has exited.

        //        fprintf(stderr, 
        //"\n"
        //"    Note that statistics below are probably worthless, since the\n"
        //"    process has already exited and they reflect the current resources\n"
        //"    used by the process.\n"
        //"\n"
        //                );
        //        fprintf(stderr, "    Working Set Size (kbytes): %u\n",
        //                (pmc.WorkingSetSize + 1023) / 1024);
        //        fprintf(stderr, "    Quota Paged Pool Usage: %u\n",
        //                pmc.QuotaPagedPoolUsage);
        //        fprintf(stderr, "    Quota Non Paged Pool Usage: %u\n",
        //                pmc.QuotaNonPagedPoolUsage);
        //        fprintf(stderr, "    Pagefile Usage: %u\n",
        //                pmc.PagefileUsage);
    } else {
        fprintf(stderr, "    GetProcessMemoryInfo() returned NULL\n");
    }
    CloseHandle(hProcess);
}


// Source for the original version of the function below, which I then
// modified slightly:
// http://www.cygwin.com/ml/cygwin/2010-02/txt00014.txt

// See also here for sample CreateProcess() code:
// http://msdn.microsoft.com/en-us/library/ms682512%28v=VS.85%29.aspx

int runCommand (char *command, STARTUPINFO *si, PROCESS_INFORMATION *pi)
{
    ZeroMemory(pi, sizeof(*pi));
    ZeroMemory(si, sizeof(*si));
    si->cb = sizeof(*si);

    if (!CreateProcessA(NULL,      // No module name (use command line)
                        command,   // Command line
                        NULL,      // Process handle not inheritable
                        NULL,      // Thread handle not inheritable
                        TRUE,      // Set (file?) handle inheritance to TRUE
                        0,         // No creation flags
                        NULL,      // Use parent's environment block
                        NULL,      // Use parent's starting directory 
                        si,        // Pointer to STARTUPINFO structure
                        pi)) {     // Pointer to PROCESS_INFORMATION structure
        return 0;
    }
    WaitForSingleObject(pi->hProcess, INFINITE);
    return 1;
}


int cleanupAfterCommandInvocation (PROCESS_INFORMATION *pi)
{
    CloseHandle(pi->hProcess);
    CloseHandle(pi->hThread);
    // insufficient when main() is called from Cygwin bash; added setbuf()
    fflush(stdout);
    fflush(stderr);
    return 1;
}


void usage (char *programName)
{
    fprintf(stderr, "usage: %s -h   to get this help\n", programName);
    fprintf(stderr, "       %s [cmdline]  to run [cmdline] and show performance stats for it when complete\n", programName);
    fprintf(stderr, "\n");
    fprintf(stderr, "Examples:\n");
    fprintf(stderr, "    From a MinGW shell window:\n");
    fprintf(stderr, "    %s 'find \"e\" medfile.txt'\n", programName);
    fprintf(stderr, "    From a Windows cmd window:\n");
    fprintf(stderr, "    %s \"find \\\"e\\\" medfile.txt\"\n", programName);
}


void _tmain (int argc, TCHAR *argv[])
{
    STARTUPINFO si;
    PROCESS_INFORMATION pi;
    int i;

    // I sometimes find it helpful to uncomment this code for testing
    // how DOS/Windows parses command lines with different styles of
    // quotes into separate words, since they definitely don't match
    // what I am familiar with (i.e. what the bash shell does).

    //    fprintf(stderr, "argc=%d  argv[] on next line:\n", argc);
    //    for (i = 0; i < argc; i++) {
    //        fprintf(stderr, " [%d]='%s'", i, argv[i]);
    //    }
    //    fprintf(stderr, "\n");

    if (argc != 2) {
        usage(argv[0]);
        exit(1);
    }

    if (!runCommand(argv[1], &si, &pi)) {
        fprintf(stderr, "CreateProcess failed (%d).\n", GetLastError());
        return;
    }

    // See if we can get stats on the process even after it exits.  I
    // don't know whether this will work or not.
    PrintMemoryAndTimeInfo(pi.dwProcessId);

    // Close process and thread handles. 
    cleanupAfterCommandInvocation(&pi);
}
