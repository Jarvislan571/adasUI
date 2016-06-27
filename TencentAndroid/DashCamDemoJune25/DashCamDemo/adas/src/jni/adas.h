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
int  adas_init(int width, int height, float side = 0.3, float upper = 0.4, float lower = 0.2);


#endif //DASHCAMDEMO_ADAS_H
