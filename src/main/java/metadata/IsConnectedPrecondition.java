/**
 * Duc Anh Nguyen
 *
 * Copyright (C) 2020-2020 by Duc Anh Nguyen and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      www.rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package metadata;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;

/**
 *
 * @author Marcin
 */
public class IsConnectedPrecondition implements Precondition{
    private InputPort port;
    private Precondition precondition;

    public IsConnectedPrecondition(InputPort port, Precondition precondition){
        this.port = port;
        this.precondition = precondition;
    }

    @Override
    public void check(MetaData metaData) {
        if (port.isConnected()){
            precondition.check(metaData);
        }
    }

    @Override
    public String getDescription() {
        return precondition.getDescription();
    }

    @Override
    public boolean isCompatible(MetaData input, CompatibilityLevel level) {
        if (port.isConnected()){
            precondition.isCompatible(input, level);
        }
        return true;
    }

    @Override
    public void assumeSatisfied() {
        if (port.isConnected()){
            precondition.assumeSatisfied();
        }
    }

    @Override
    public MetaData getExpectedMetaData() {
        if (port.isConnected()){
            return precondition.getExpectedMetaData();
        }
        return new ExampleSetMetaData();
    }

}
