/*
 * LaneDetector.h
 *
 *  Created on: Jun 3, 2016
 *      Author: windywinter
 */

#ifndef LANEDETECTOR_H_
#define LANEDETECTOR_H_

#include <stdint.h>

struct LaneDetectorParams {
	int ROI_x = 480;                         //W*0.25
	int ROI_y = 756;                         //H*0.7
	int ROI_width = 998;                     //W*0.52
	int ROI_height = 313;                    //H*0.29
	float detector_factor = 2.0;
	int gaussian_sigma_x = 6;                //Typical pixel width of the lane mark
	int gaussian_sigma_y = 37;               //Typical pixel height of the lane mark. Must be an odd number
	float res_threshold = 97.5;
	float groupping_threshold = 60.0;        //The smaller, the fewer peak points there will be
	float peak_filter_alpha = 0.9;           //Memory effect of the threshold image. The small, the less it will memorize.
	float line_fit_range = 40.0;             //Pixel range in x direction for line fitting
	float line_fit_minimal_fit_ratio = 0.2;  //Minimal white pixel ratio for line fitting
};

struct Lane{
	int x1, y1;
	int x2, y2;
};

bool init_lane_detection(uint32_t srcWidth, uint32_t srcHeight);
bool init_lane_detection(uint32_t srcWidth, uint32_t srcHeight, const LaneDetectorParams & params);
bool lane_detection_process(uint8_t * __restrict src, Lane * lanes, unsigned int * count, unsigned int * departure);
bool deinit_lane_detection();

#endif /* LANEDETECTOR_H_ */
