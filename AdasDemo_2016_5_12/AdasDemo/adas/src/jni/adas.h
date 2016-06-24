#ifndef DASHCAMDEMO_ADAS_H
#define DASHCAMDEMO_ADAS_H
#include <stdint.h>
typedef struct _tagCarDistance {
    int x;
    int y;
    int width;
    int height;
    int distance;    
} CarDistance;


int  adas_car_detect(uint8_t *frame, CarDistance *distance, unsigned int *count,float *ttc);
void adas_free();
int  adas_init(int width, int height);
#endif //DASHCAMDEMO_ADAS_H
