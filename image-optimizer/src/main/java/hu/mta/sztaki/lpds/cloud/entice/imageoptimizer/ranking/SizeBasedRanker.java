/*
 *    Copyright 2009-2015 Gabor Kecskemeti, University of Westminster, MTA SZTAKI
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.mta.sztaki.lpds.cloud.entice.imageoptimizer.ranking;

import hu.mta.sztaki.lpds.cloud.entice.imageoptimizer.ItemPool;
import hu.mta.sztaki.lpds.cloud.entice.imageoptimizer.grouping.Group;

public class SizeBasedRanker extends Ranker {

	@Override
	public double rank(Group g) {
//		return (double) g.getSize() / ItemPool.getInstance().getOverallSize();
		double sizeRank = (double) g.getSize() / ItemPool.getInstance().getOverallSize();
		return sizeRank * g.getWeight();
	}

}
