/* This small, original ARM host marker lets Android select its supported ARM runtime.
 * It contains no spider, subscription, media, credential or platform modification. */
__attribute__((visibility("default"))) int ptvbox_spider_host_version(void) { return 1; }
